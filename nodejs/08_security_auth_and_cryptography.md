# Part 8: Security, Cryptography, Authentication & Hardening (Q166 - Q185)

---

### Q166: What is Prototype Pollution and how does it compromise Node.js applications?
**Answer:**
Prototype Pollution occurs when user-controlled input modifies `Object.prototype`, injecting or altering properties on **all** JavaScript objects created across the entire Node.js runtime.

```javascript
// Malicious JSON payload:
// { "__proto__": { "isAdmin": true } }

function deepMerge(target, source) {
  for (const key in source) {
    if (typeof source[key] === 'object' && source[key] !== null) {
      if (!target[key]) target[key] = {};
      deepMerge(target[key], source[key]); // 🚨 VULNERABLE to prototype pollution!
    } else {
      target[key] = source[key];
    }
  }
  return target;
}
```

**Defense & Hardening:**
1. Block dangerous keys: `key === '__proto__' || key === 'constructor' || key === 'prototype'`.
2. Use objects with no prototype: `Object.create(null)` or `Map`.
3. Freeze prototype: `Object.freeze(Object.prototype)` or launch Node with `--disable-proto=delete` or `--disable-proto=throw`.

---

### Q167: What is Timing Attack and how do you use `crypto.timingSafeEqual()` to defend against it?
**Answer:**
Standard string comparison `strA === strB` returns `false` on the **first mismatched character** (fail-fast).
An attacker measuring nanosecond response times can guess passwords, API keys, or HMAC signatures character-by-character.

`crypto.timingSafeEqual(bufA, bufB)` executes in constant time regardless of where or whether characters differ.

```javascript
import crypto from 'node:crypto';

function secureCompare(userInput, secretToken) {
  const userBuf = Buffer.from(userInput);
  const secretBuf = Buffer.from(secretToken);

  // Buffers MUST be equal length for timingSafeEqual
  if (userBuf.length !== secretBuf.length) {
    // Hash both to equalize lengths before timingSafeEqual to avoid length oracle
    const hashA = crypto.createHash('sha256').update(userBuf).digest();
    const hashB = crypto.createHash('sha256').update(secretBuf).digest();
    crypto.timingSafeEqual(hashA, hashB);
    return false;
  }

  return crypto.timingSafeEqual(userBuf, secretBuf);
}
```

---

### Q168: How do you safely hash and verify passwords using `crypto.scrypt` or `argon2`?
**Answer:**
Never use fast cryptographic hashes (MD5, SHA-256) for passwords; GPUs can compute billions of SHA-256 hashes per second. Use memory-hard key derivation functions like **Argon2id** or **Scrypt**.

```javascript
import crypto from 'node:crypto';
import util from 'node:util';

const scrypt = util.promisify(crypto.scrypt);

async function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const derivedKey = await scrypt(password, salt, 64, {
    N: 16384, // CPU/memory cost
    r: 8,     // Block size
    p: 1      // Parallelization
  });
  return `${salt}:${derivedKey.toString('hex')}`;
}

async function verifyPassword(password, storedHash) {
  const [salt, key] = storedHash.split(':');
  const keyBuffer = Buffer.from(key, 'hex');
  const derivedKey = await scrypt(password, salt, 64, { N: 16384, r: 8, p: 1 });
  return crypto.timingSafeEqual(keyBuffer, derivedKey);
}
```

---

### Q169: What is ReDoS (Regular Expression Denial of Service) and how do you mitigate it?
**Answer:**
ReDoS occurs when a vulnerable regular expression with "evil regex" patterns (nested quantifiers like `(a+)+$`) experiences **catastrophic backtracking** on crafted non-matching input strings, locking the Event Loop at 100% CPU.

```javascript
// 🚨 VULNERABLE Regex:
const evilRegex = /(a+)+$/;
evilRegex.test('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaX'); // Freezes Node process for minutes!
```

**Mitigations:**
1. Audit regex with linters (`eslint-plugin-regexp`).
2. Set regexp backtrack limits in V8.
3. Use linear-time regular expression engines (e.g. `re2`).

---

### Q170: How do you implement Secure AES-256-GCM Encryption and Decryption in Node.js?
**Answer:**
AES-GCM (Galois/Counter Mode) provides **Authenticated Encryption with Associated Data (AEAD)**, ensuring both confidentiality and tamper-proof data integrity.

```javascript
import crypto from 'node:crypto';

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 12; // 96-bit IV recommended for GCM
const KEY = crypto.randomBytes(32); // 256-bit key

function encrypt(plainText) {
  const iv = crypto.randomBytes(IV_LENGTH);
  const cipher = crypto.createCipheriv(ALGORITHM, KEY, iv);

  let encrypted = cipher.update(plainText, 'utf8', 'hex');
  encrypted += cipher.final('hex');
  const authTag = cipher.getAuthTag().toString('hex'); // 16-byte authentication tag

  return {
    iv: iv.toString('hex'),
    encryptedData: encrypted,
    authTag
  };
}

function decrypt({ encryptedData, iv, authTag }) {
  const decipher = crypto.createDecipheriv(ALGORITHM, KEY, Buffer.from(iv, 'hex'));
  decipher.setAuthTag(Buffer.from(authTag, 'hex')); // Throws error if data was tampered!

  let decrypted = decipher.update(encryptedData, 'hex', 'utf8');
  decrypted += decipher.final('utf8');
  return decrypted;
}
```

---

### Q171: How do you sign and verify JWTs using Asymmetric RS256/ES256 instead of Symmetric HS256?
**Answer:**
- **HS256 (Shared Secret)**: Both the authentication server (signer) and every microservice (verifier) must share the secret key. If one service is compromised, attackers can forge valid tokens.
- **RS256 / ES256 (Public/Private Key)**: Only the Auth service holds the private key to sign. Downstream microservices only need the public key to verify signatures.

```javascript
import crypto from 'node:crypto';

// Generate asymmetric elliptic curve keypair
const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', {
  namedCurve: 'prime256v1'
});

// Signing
function signPayload(data) {
  const signer = crypto.createSign('SHA256');
  signer.update(JSON.stringify(data));
  return signer.sign(privateKey, 'base64url');
}

// Verification
function verifyPayload(data, signature) {
  const verifier = crypto.createVerify('SHA256');
  verifier.update(JSON.stringify(data));
  return verifier.verify(publicKey, signature, 'base64url');
}
```

---

### Q172: What are essential HTTP Security Headers and how does `helmet` configure them?
**Answer:**

```javascript
import helmet from 'helmet';
import express from 'express';

const app = express();

app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'trusted-cdn.com'"],
      objectSrc: ["'none'"]
    }
  },
  strictTransportSecurity: {
    maxAge: 63072000, // 2 years
    includeSubDomains: true,
    preload: true
  },
  frameguard: { action: 'deny' }, // X-Frame-Options: Clickjacking defense
  noSniff: true                   // X-Content-Type-Options: nosniff
}));
```

---

### Q173: How do you prevent SQL Injection and NoSQL Injection in Node.js?
**Answer:**
1. **SQL**: Always use **Parameterized Prepared Statements** (never concatenate strings!).
   ```javascript
   // ✅ SECURE Parameterized Query
   await pool.query('SELECT * FROM users WHERE email = $1', [email]);
   ```
2. **NoSQL (MongoDB/Mongoose)**: Sanitize object queries (prevent `$gt: ""` operator injection).
   ```javascript
   // ❌ VULNERABLE: req.body.password = { "$gt": "" } matches all users!
   User.findOne({ email: req.body.email, password: req.body.password });

   // ✅ SECURE: Sanitize input or force string types:
   User.findOne({
     email: String(req.body.email),
     password: String(req.body.password)
   });
   ```

---

### Q174: How do you implement Cross-Site Request Forgery (CSRF) Protection using SameSite Cookies and Double-Submit Tokens?
**Answer:**
1. **`SameSite=Strict` / `SameSite=Lax` Cookies**: Modern browsers automatically block cookies from being sent on cross-site sub-requests.
2. **Double-Submit Cookie Pattern**: Server sets a cryptographically random token in both a cookie and an HTTP header (`x-csrf-token`). Incoming mutation requests verify both match.

---

### Q175: What is Server-Side Request Forgery (SSRF) and how do you prevent it in Node.js fetch clients?
**Answer:**
SSRF occurs when an attacker forces the Node.js server to make outbound HTTP requests to internal cloud metadata IP addresses (`http://169.254.169.254/latest/meta-data/`) or internal network IPs (`10.0.0.0/8`, `192.168.0.0/16`, `127.0.0.1`).

**Defense**:
Resolve DNS before making request and validate that the resolved IP does not belong to private CIDR blocks.

```javascript
import dns from 'node:dns/promises';
import ipaddr from 'ipaddr.js';

async function safeFetch(urlStr) {
  const parsed = new URL(urlStr);
  const { address } = await dns.lookup(parsed.hostname);

  const ip = ipaddr.parse(address);
  if (ip.range() !== 'unicast') {
    throw new Error('Forbidden: SSRF attempt to internal/private IP blocked!');
  }

  return await fetch(urlStr);
}
```

---

### Q176: How do you securely handle Secrets and Environment Variables in Node.js without leaking to child processes or logs?
**Answer:**
- Use Node.js v20.6+ built-in `--env-file=.env` (avoids third-party `dotenv` dependency).
- Never log `process.env`.
- Explicitly pass whitelisted variables to child processes (`spawn(cmd, args, { env: whitelist })`).
- Fetch dynamic secrets at runtime from AWS Secrets Manager / HashiCorp Vault.

---

### Q177: What is Dependency Confusion and Typosquatting in NPM and how do you protect against it?
**Answer:**
- **Dependency Confusion**: Attacker registers a public npm package with the same name as a company's internal private package. If npm registry resolution is misconfigured, npm pulls the malicious public package.
- **Defenses**:
  1. Scope all internal packages (`@my-org/auth-lib`).
  2. Commit `package-lock.json` and enforce `npm ci` in CI/CD.
  3. Use `.npmrc` with strict scoping and private registry proxies (Artifactory/Nexus).

---

### Q178: How do you sanitize HTML to prevent Stored & Reflected XSS using `DOMPurify` / `sanitize-html`?
**Answer:**

```javascript
import sanitizeHtml from 'sanitize-html';

const dirty = '<script>alert("xss")</script><b>Hello</b> <a href="javascript:steal()">Click</a>';
const clean = sanitizeHtml(dirty, {
  allowedTags: ['b', 'i', 'em', 'strong', 'a'],
  allowedAttributes: { 'a': ['href'] },
  allowedSchemes: ['http', 'https', 'mailto'] // Blocks javascript: URLs
});

console.log(clean); // <b>Hello</b> <a>Click</a>
```

---

### Q179: What is the Node.js Policy System (`--experimental-policy`)?
**Answer:**
Allows enforcing strict cryptographic integrity checks on every loaded module via `policy.json`. If a module in `node_modules` is tampered with or dependencies are altered, Node.js terminates execution immediately.

```json
{
  "resources": {
    "./app.js": {
      "integrity": "sha384-xyz..."
    }
  }
}
```

---

### Q180: How do you implement Secure Session Management with `httpOnly`, `secure`, and `sameSite` cookies?
**Answer:**

```javascript
import session from 'express-session';
import RedisStore from 'connect-redis';

app.use(session({
  store: new RedisStore({ client: redisClient }),
  name: '__Host-sessionid', // Prefix ensures Secure + root path enforcement
  secret: process.env.SESSION_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,        // Blocks document.cookie XSS access
    secure: true,          // Enforces HTTPS only
    sameSite: 'strict',    // CSRF defense
    maxAge: 1000 * 60 * 60 // 1 hour expiration
  }
}));
```

---

### Q181: How do you prevent Path Traversal attacks in file-serving APIs?
**Answer:**
Attackers pass `../../../../etc/passwd` to file download routes.

```javascript
import path from 'node:path';
import fs from 'node:fs';

const SAFE_ROOT = '/var/www/uploads';

function serveUserFile(filename, res) {
  // Resolve absolute path
  const safePath = path.resolve(SAFE_ROOT, filename);

  // Verify that resolved path begins with SAFE_ROOT directory
  if (!safePath.startsWith(SAFE_ROOT + path.sep)) {
    res.statusCode = 403;
    return res.end('Access Denied');
  }

  fs.createReadStream(safePath).pipe(res);
}
```

---

### Q182: What is AST Injection and Dynamic Code Execution vulnerability (`eval`, `new Function`, `vm`)?
**Answer:**
Executing user-supplied strings inside `eval()`, `new Function()`, or even Node's built-in `vm.runInContext()` allows attackers to break out and execute arbitrary OS commands.
- `vm` in Node.js is **NOT a security sandbox**; code running inside a VM can access the host `Function` constructor and execute `process.exit()`.
- Use isolated V8 engines like `isolated-vm` if running untrusted JavaScript.

---

### Q183: How do you enforce Mutual TLS (mTLS) between Node.js Microservices?
**Answer:**
mTLS requires both client and server to present and verify each other's X.509 SSL certificates against a private Certificate Authority (CA).

```javascript
import https from 'node:https';
import fs from 'node:fs';

const server = https.createServer({
  key: fs.readFileSync('server.key'),
  cert: fs.readFileSync('server.crt'),
  ca: fs.readFileSync('ca.crt'),
  requestCert: true,        // Demand client certificate
  rejectUnauthorized: true  // Reject handshake if client cert is invalid/untrusted
}, (req, res) => {
  const clientCert = req.socket.getPeerCertificate();
  console.log('Authenticated microservice client:', clientCert.subject.CN);
  res.end('mTLS Handshake Verified');
});

server.listen(443);
```

---

### Q184: How do you securely generate Cryptographically Strong Random Identifiers in Node.js?
**Answer:**
Never use `Math.random()` for tokens, session IDs, or password reset links (it is pseudo-random and predictable).
Use `crypto.randomBytes()`, `crypto.randomUUID()`, or `crypto.getRandomValues()`.

```javascript
import crypto from 'node:crypto';

const uuid = crypto.randomUUID(); // Fast v4 UUID
const randomToken = crypto.randomBytes(32).toString('hex'); // 256-bit cryptographically secure token
```

---

### Q185: What is Content Security Policy (CSP) Nonce and how is it generated per-request in Node.js?
**Answer:**
CSP Nonce generates a unique cryptographically random token per HTTP request and embeds it in the `Content-Security-Policy` header. Only inline `<script nonce="...">` tags matching the nonce are executed by the browser, blocking all inline XSS injections.
