# 后端企业微信登录接口开发计划

## 一、接口规范

### 1.1 接口信息

| 项目 | 说明 |
|-----|------|
| 路径 | `POST /auth/wxwork/login` |
| 认证 | 无需认证（登录接口） |
| Content-Type | `application/json` |

### 1.2 请求参数

```json
{
  "code": "企业微信授权返回的code（一次性，5分钟有效）"
}
```

### 1.3 响应格式

**成功：**
```json
{
  "code": 200,
  "msg": "登录成功",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**失败 - 用户不存在：**
```json
{
  "code": 40001,
  "msg": "未找到对应的系统用户，请联系管理员"
}
```

**失败 - code 无效：**
```json
{
  "code": 40002,
  "msg": "授权码无效或已过期"
}
```

---

## 二、企业微信 API 调用流程

```
┌─────────────────────────────────────────────────────────────┐
│                    后端处理流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 接收前端传来的 code                                      │
│           ↓                                                 │
│  2. 获取 access_token（建议缓存，有效期 7200 秒）             │
│     GET https://qyapi.weixin.qq.com/cgi-bin/gettoken        │
│         ?corpid={CORP_ID}                                   │
│         &corpsecret={CORP_SECRET}                           │
│           ↓                                                 │
│  3. 用 code 换取用户身份                                     │
│     GET https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo│
│         ?access_token={ACCESS_TOKEN}                        │
│         &code={CODE}                                        │
│           ↓                                                 │
│  4. 根据返回的 UserId 查询系统用户                           │
│     SELECT * FROM sys_user WHERE user_name = {UserId}       │
│           ↓                                                 │
│  5. 生成 JWT token 返回                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、企业微信 API 详情

### 3.1 获取 access_token

**请求：**
```
GET https://qyapi.weixin.qq.com/cgi-bin/gettoken
    ?corpid={CORP_ID}
    &corpsecret={CORP_SECRET}
```

**响应：**
```json
{
  "errcode": 0,
  "errmsg": "ok",
  "access_token": "accesstoken000001",
  "expires_in": 7200
}
```

> **注意**：access_token 有效期 2 小时，建议使用 Redis 缓存，避免频繁请求

### 3.2 获取用户身份

**请求：**
```
GET https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo
    ?access_token={ACCESS_TOKEN}
    &code={CODE}
```

**响应（企业成员）：**
```json
{
  "errcode": 0,
  "errmsg": "ok",
  "userid": "zhangsan",
  "user_ticket": "xxx"
}
```

**响应（非企业成员）：**
```json
{
  "errcode": 0,
  "errmsg": "ok",
  "openid": "xxx"
}
```

---

## 四、伪代码参考

```java
@PostMapping("/auth/wxwork/login")
public Result wxworkLogin(@RequestBody WxworkLoginRequest request) {
    String code = request.getCode();

    // 1. 获取 access_token（优先从缓存获取）
    String accessToken = wxworkService.getAccessToken();

    // 2. 用 code 换取用户身份
    WxworkUserInfo userInfo = wxworkService.getUserInfo(accessToken, code);

    if (userInfo == null || userInfo.getUserid() == null) {
        return Result.error(40002, "授权码无效或已过期");
    }

    // 3. 根据 userid 查询系统用户（假设 userid 与系统用户名一致）
    SysUser user = userService.findByUserName(userInfo.getUserid());

    if (user == null) {
        return Result.error(40001, "未找到对应的系统用户，请联系管理员");
    }

    // 4. 生成 JWT token（复用现有的 token 生成逻辑）
    String token = tokenService.createToken(user);

    return Result.success("登录成功", token);
}
```

---

## 五、配置信息

后端需要配置以下参数（建议放在配置文件中）：

```yaml
# application.yml
wxwork:
  corp-id: "企业ID"           # 用户提供
  corp-secret: "应用Secret"   # 用户提供
  agent-id: "应用AgentId"     # 用户��供
```

---

## 六、用户映射方式

| 方案 | 说明 |
|-----|------|
| **推荐** | 企微 `userid` 与系统 `user_name`（工号）一致，直接匹配 |
| 备选 | 在 `sys_user` 表新增 `wxwork_userid` 字段存储映射关系 |

如果采用备选方案，需要执行：
```sql
ALTER TABLE sys_user ADD COLUMN wxwork_userid VARCHAR(64) COMMENT '企业微信用户ID';
CREATE UNIQUE INDEX idx_wxwork_userid ON sys_user(wxwork_userid);
```

---

## 七、企业微信错误码参考

| errcode | 说明 |
|---------|------|
| 0 | 成功 |
| 40014 | 不合法的 access_token |
| 40029 | 不合法的 oauth_code |
| 40068 | 不合法的 agentid |
| 42001 | access_token 已过期 |
| 50001 | redirect_uri 未登记可信域名 |

---

## 八、注意事项

1. **code 只能使用一次**，5 分钟内有效
2. **access_token 需要缓存**，避免频繁请求导致限流
3. **错误码处理**：企微 API 返回 `errcode != 0` 时需要记录日志
4. **安全性**：`corp_secret` 只能存在后端，不能暴露给前端

---

## 九、测试建议

1. 使用企业微信开发者工具模拟授权流程
2. 测试 code 过期场景
3. 测试用户不存在场景
4. 测试 access_token 缓存刷新

---

## 十、相关文档

- [企业微信开发文档 - 网页授权登录](https://developer.work.weixin.qq.com/document/path/91335)
- [企业微信开发文档 - 获取访问用户身份](https://developer.work.weixin.qq.com/document/path/91023)
