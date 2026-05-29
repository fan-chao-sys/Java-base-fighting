# Day5 Session、Cookie、Token、鉴权流程 底层核心知识点背诵笔记 ⭐⭐

---

## 一、Cookie、Session、Token 对比

| | Cookie | Session | Token（JWT） |
|---|---|---|---|
| **存储位置** | **浏览器** | **服务端**（内存/Redis） | **客户端**（localStorage/Header） |
| **大小限制** | 4KB | 无限制（取决于服务端） | 无限制（但不宜过大） |
| **安全性** | 低（明文/可篡改） | 较高（服务端控制） | 较高（签名防篡改） |
| **服务端存储** | 不占 | **占内存** | **不占**（无状态） |
| **分布式扩展** | 天然支持 | 需共享（Redis/粘性Session） | **天然支持** |
| **主动失效** | 客户端可删 | ✅ 服务端直接删 | ❌ 需配合黑名单/短过期 |
| **适用场景** | 传统 Web（服务端渲染） | 传统 Web 应用 | **SPA / 移动端 / 微服务** |

---

## 二、JWT 结构

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx_signature
    │                      │              │
  Header                 Payload       Signature

Header：  {"alg":"HS256", "typ":"JWT"}
Payload： {"userId":1, "exp":1700000000}
Signature： HMAC-SHA256( Base64(Header) + "." + Base64(Payload), secret )
```

**JWT 防篡改原理**：服务端用 secret 对 Header+Payload 签名生成 Signature。客户端改了 Payload → 签名不匹配 → 服务端拒绝。

---

## 三、认证流程对比

### Session 认证
```
客户端                   服务端
  │── login(user,pwd) ──→│ 验证 → 创建 Session → 存 Redis
  │←─ Set-Cookie: SID ──│
  │── GET /api 带 Cookie →│ 查 Redis 验证 Session
```

### JWT 认证
```
客户端                   服务端
  │── login(user,pwd) ──→│ 验证 → 生成 JWT
  │←────── JWT ────────│
  │── GET /api ──────────→│ 验证签名 + 过期时间
  │   Authorization:       │
  │   Bearer <JWT>         │
```

---

## 四、SSO 单点登录

| 方案 | 原理 |
|---|---|
| **CAS** | 中心认证服务，所有系统跳转到同一个登录中心 |
| **OAuth 2.0** | 授权码模式，第三方授权（微信登录） |
| **JWT 跨域** | 同一域名下共享 JWT，不同域名通过 iframe/postMessage |

---

## 五、终极背诵总结

1. **Cookie**：存浏览器，自动带，4KB 小容量
2. **Session**：存服务端，SessionID 关联，分布式需共享 Redis
3. **JWT**：无状态，Header+Payload+Signature，适合分布式/移动端
4. **JWT 缺点**：无法主动失效（除非短过期+黑名单）
5. **认证选型**：传统 Web→Session，SPA/移动端/微服务→JWT
