import { createClient } from "npm:@supabase/supabase-js@2.105.0";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const PUBLISHABLE_KEYS = JSON.parse(Deno.env.get("SUPABASE_PUBLISHABLE_KEYS") ?? "{}");
const SECRET_KEYS = JSON.parse(Deno.env.get("SUPABASE_SECRET_KEYS") ?? "{}");
const PUBLISHABLE_KEY = PUBLISHABLE_KEYS.default ?? Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const SECRET_KEY = SECRET_KEYS.default ?? Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const allowedOrigins = new Set([
  "https://khayom1.github.io",
  "http://localhost:8000",
  "http://127.0.0.1:8000",
]);

const corsHeaders = (origin: string) => ({
  "Access-Control-Allow-Origin": allowedOrigins.has(origin) ? origin : "https://khayom1.github.io",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Max-Age": "86400",
  "Vary": "Origin",
});

const json = (body: Record<string, unknown>, status: number, origin: string) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders(origin),
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "Pragma": "no-cache",
    },
  });

const emailForLogin = (login: string) => `${login}@accounts.shahrvand.tj`;

async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest)).map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function normaliseLogin(value: unknown): string {
  return typeof value === "string" ? value.trim().toLowerCase() : "";
}

function validName(value: unknown): value is string {
  return typeof value === "string" &&
    value.trim().length >= 1 &&
    value.trim().length <= 60 &&
    !/[\u0000-\u001f\u007f]/.test(value);
}

function validOptionalName(value: unknown): value is string | null | undefined {
  return value == null || value === "" || (
    typeof value === "string" &&
    value.trim().length <= 60 &&
    !/[\u0000-\u001f\u007f]/.test(value)
  );
}

function clients() {
  if (!SUPABASE_URL || !PUBLISHABLE_KEY || !SECRET_KEY) {
    throw new Error("Supabase Edge Function credentials are not configured.");
  }
  const admin = createClient(SUPABASE_URL, SECRET_KEY, {
    auth: { autoRefreshToken: false, persistSession: false, detectSessionInUrl: false },
  });
  const publicClient = createClient(SUPABASE_URL, PUBLISHABLE_KEY, {
    auth: { autoRefreshToken: false, persistSession: false, detectSessionInUrl: false },
  });
  return { admin, publicClient };
}

Deno.serve(async (req: Request) => {
  const origin = req.headers.get("origin") ?? "";
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders(origin) });
  }
  if (req.method !== "POST") {
    return json({ error: "METHOD_NOT_ALLOWED" }, 405, origin);
  }
  if (origin !== "" && !allowedOrigins.has(origin)) {
    return json({ error: "ORIGIN_NOT_ALLOWED" }, 403, origin);
  }

  const declaredLength = Number(req.headers.get("content-length") ?? "0");
  if (declaredLength > 8192) {
    return json({ error: "REQUEST_TOO_LARGE" }, 413, origin);
  }

  let input: Record<string, unknown>;
  try {
    const raw = await req.text();
    if (raw.length > 8192) return json({ error: "REQUEST_TOO_LARGE" }, 413, origin);
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("Invalid JSON");
    input = parsed as Record<string, unknown>;
  } catch {
    return json({ error: "INVALID_REQUEST" }, 400, origin);
  }

  const action = typeof input.action === "string" &&
    ["register", "login", "update_profile", "change_password"].includes(input.action)
    ? input.action
    : null;
  if (!action) return json({ error: "INVALID_ACTION" }, 400, origin);

  if (action === "update_profile" || action === "change_password") {
    const authorization = req.headers.get("authorization") ?? "";
    const token = authorization.match(/^Bearer\\s+(.+)$/i)?.[1] ?? "";
    if (!token) return json({ error: "INVALID_SESSION" }, 401, origin);

    try {
      const { admin, publicClient } = clients();
      const { data: authData, error: authError } = await admin.auth.getUser(token);
      const user = authData.user;
      if (authError || !user) return json({ error: "INVALID_SESSION" }, 401, origin);

      if (action === "change_password") {
        const currentPassword = typeof input.current_password === "string" ? input.current_password : "";
        const newPassword = typeof input.new_password === "string" ? input.new_password : "";
        if (!currentPassword || newPassword.length < 8 || newPassword.length > 128) {
          return json({ error: "INVALID_PASSWORD" }, 400, origin);
        }
        if (!user.email) return json({ error: "INVALID_SESSION" }, 401, origin);

        const { data: verified, error: verifyError } = await publicClient.auth.signInWithPassword({
          email: user.email,
          password: currentPassword,
        });
        if (verifyError || !verified.user) {
          return json({ error: "INVALID_CURRENT_PASSWORD" }, 401, origin);
        }

        const { error: passwordError } = await admin.auth.admin.updateUserById(user.id, {
          password: newPassword,
        });
        if (passwordError) {
          console.error("Password update failed:", passwordError.message);
          return json({ error: "PASSWORD_UPDATE_FAILED" }, 500, origin);
        }
        return json({ ok: true }, 200, origin);
      }

      const firstName = input.first_name;
      const lastName = input.last_name;
      const age = Number(input.age);
      const nextLogin = normaliseLogin(input.login);
      if (!validName(firstName) || !validOptionalName(lastName)) {
        return json({ error: "INVALID_NAME" }, 400, origin);
      }
      if (!Number.isInteger(age) || age < 7 || age > 120) {
        return json({ error: "INVALID_AGE" }, 400, origin);
      }
      if (!/^[a-z0-9][a-z0-9._-]{3,23}$/.test(nextLogin)) {
        return json({ error: "INVALID_LOGIN" }, 400, origin);
      }

      const { data: currentProfile, error: currentProfileError } = await admin
        .from("shahrvand_profiles")
        .select("id, first_name, last_name, age, login")
        .eq("id", user.id)
        .maybeSingle();
      if (currentProfileError || !currentProfile) {
        console.error("Current profile lookup failed:", currentProfileError?.message ?? "Profile not found");
        return json({ error: "PROFILE_UPDATE_FAILED" }, 404, origin);
      }

      if (nextLogin !== currentProfile.login) {
        const { data: existing, error: lookupError } = await admin
          .from("shahrvand_profiles")
          .select("id")
          .eq("login", nextLogin)
          .neq("id", user.id)
          .maybeSingle();
        if (lookupError) {
          console.error("Login availability check failed:", lookupError.message);
          return json({ error: "SERVICE_TEMPORARILY_UNAVAILABLE" }, 503, origin);
        }
        if (existing) return json({ error: "LOGIN_ALREADY_USED" }, 409, origin);
      }

      const nextLastName = typeof lastName === "string" ? (lastName.trim() || null) : null;
      const metadata = {
        ...(user.user_metadata ?? {}),
        first_name: firstName.trim(),
        last_name: nextLastName,
        age,
        login: nextLogin,
      };
      const { error: authUpdateError } = await admin.auth.admin.updateUserById(user.id, {
        email: emailForLogin(nextLogin),
        email_confirm: true,
        user_metadata: metadata,
      });
      if (authUpdateError) {
        console.error("Auth profile update failed:", authUpdateError.message);
        if (authUpdateError.code === "email_exists") {
          return json({ error: "LOGIN_ALREADY_USED" }, 409, origin);
        }
        return json({ error: "PROFILE_UPDATE_FAILED" }, 500, origin);
      }

      const { data: updatedProfile, error: profileUpdateError } = await admin
        .from("shahrvand_profiles")
        .update({
          first_name: firstName.trim(),
          last_name: nextLastName,
          age,
          login: nextLogin,
        })
        .eq("id", user.id)
        .select("id, first_name, last_name, age, login")
        .single();

      if (profileUpdateError || !updatedProfile) {
        await admin.auth.admin.updateUserById(user.id, {
          email: emailForLogin(currentProfile.login),
          email_confirm: true,
          user_metadata: {
            ...(user.user_metadata ?? {}),
            first_name: currentProfile.first_name,
            last_name: currentProfile.last_name,
            age: currentProfile.age,
            login: currentProfile.login,
          },
        });
        console.error("Profile table update failed:", profileUpdateError?.message ?? "No profile returned");
        if (profileUpdateError?.code === "23505") {
          return json({ error: "LOGIN_ALREADY_USED" }, 409, origin);
        }
        return json({ error: "PROFILE_UPDATE_FAILED" }, 500, origin);
      }

      return json({ ok: true, profile: updatedProfile }, 200, origin);
    } catch (error) {
      console.error("Authenticated account update failed:", error);
      return json({ error: "SERVICE_TEMPORARILY_UNAVAILABLE" }, 500, origin);
    }
  }

  const login = normaliseLogin(input.login);
  const password = typeof input.password === "string" ? input.password : "";
  if (!/^[a-z0-9][a-z0-9._-]{3,23}$/.test(login)) {
    return json({ error: "INVALID_LOGIN" }, 400, origin);
  }
  if (password.length < 8 || password.length > 128) {
    return json({ error: "INVALID_PASSWORD" }, 400, origin);
  }

  const rawIp = req.headers.get("cf-connecting-ip") ??
    req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ??
    "unknown";
  const ipHash = await sha256(`${SUPABASE_URL}:${rawIp}`);

  try {
    const { admin, publicClient } = clients();

    const { data: permitted, error: rateError } = await admin.rpc(
      "shahrvand_check_auth_rate_limit",
      {
        p_ip_hash: ipHash,
        p_action: action,
        p_limit: action === "register" ? 5 : 10,
      },
    );
    if (rateError) {
      console.error("Auth rate-limit check failed:", rateError.message);
      return json({ error: "SERVICE_TEMPORARILY_UNAVAILABLE" }, 503, origin);
    }
    if (permitted !== true) {
      return json({ error: "RATE_LIMITED" }, 429, origin);
    }

    const email = emailForLogin(login);

    if (action === "register") {
      const firstName = input.first_name;
      const lastName = input.last_name;
      const age = Number(input.age);

      if (!validName(firstName) || !validOptionalName(lastName)) {
        return json({ error: "INVALID_NAME" }, 400, origin);
      }
      if (!Number.isInteger(age) || age < 7 || age > 120) {
        return json({ error: "INVALID_AGE" }, 400, origin);
      }

      const { data: existing, error: lookupError } = await admin
        .from("shahrvand_profiles")
        .select("id")
        .eq("login", login)
        .maybeSingle();
      if (lookupError) {
        console.error("Profile lookup failed:", lookupError.message);
        return json({ error: "SERVICE_TEMPORARILY_UNAVAILABLE" }, 503, origin);
      }
      if (existing) return json({ error: "LOGIN_ALREADY_USED" }, 409, origin);

      const { data: created, error: createError } = await admin.auth.admin.createUser({
        email,
        password,
        email_confirm: true,
        user_metadata: {
          first_name: firstName.trim(),
          last_name: typeof lastName === "string" ? (lastName.trim() || null) : null,
          age,
          login,
        },
      });
      if (createError || !created.user) {
        console.error("Auth account creation failed:", createError?.message ?? "No user returned");
        return json({ error: "ACCOUNT_CREATION_FAILED" }, 409, origin);
      }

      const { error: profileError } = await admin.from("shahrvand_profiles").insert({
        id: created.user.id,
        first_name: firstName.trim(),
        last_name: typeof lastName === "string" ? (lastName.trim() || null) : null,
        age,
        login,
      });

      if (profileError) {
        await admin.auth.admin.deleteUser(created.user.id);
        console.error("Profile insert failed:", profileError.message);
        if (profileError.code === "23505") return json({ error: "LOGIN_ALREADY_USED" }, 409, origin);
        return json({ error: "PROFILE_CREATION_FAILED" }, 500, origin);
      }

      const { data: signInData, error: signInError } = await publicClient.auth.signInWithPassword({
        email,
        password,
      });
      if (signInError || !signInData.session) {
        console.error("Sign-in after registration failed:", signInError?.message ?? "No session returned");
        return json({ ok: true, account_created: true, needs_login: true }, 201, origin);
      }

      return json({
        ok: true,
        profile: {
          id: created.user.id,
          first_name: firstName.trim(),
          last_name: typeof lastName === "string" ? (lastName.trim() || null) : null,
          age,
          login,
        },
        session: signInData.session,
      }, 201, origin);
    }

    const { data: profile, error: profileError } = await admin
      .from("shahrvand_profiles")
      .select("id, first_name, last_name, age, login")
      .eq("login", login)
      .maybeSingle();

    if (profileError || !profile) {
      return json({ error: "INVALID_CREDENTIALS" }, 401, origin);
    }

    const { data: signInData, error: signInError } = await publicClient.auth.signInWithPassword({
      email,
      password,
    });
    if (signInError || !signInData.session) {
      return json({ error: "INVALID_CREDENTIALS" }, 401, origin);
    }

    return json({
      ok: true,
      profile,
      session: signInData.session,
    }, 200, origin);
  } catch (error) {
    console.error("SHAHRVAND auth handler failed:", error);
    return json({ error: "SERVICE_TEMPORARILY_UNAVAILABLE" }, 500, origin);
  }
});
