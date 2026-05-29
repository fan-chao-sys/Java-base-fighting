import static com.yc.Xcommon.SimpleJwtUtil.generateToken;
import static com.yc.Xcommon.SimpleJwtUtil.validateToken;

public class W7D5_Actual {

    //
    //写一个简单的 JWT 生成和验证逻辑（不依赖库，用 HMAC-SHA256）
    public static void main(String[] args) throws Exception {
        String token = generateToken("user123");
        System.out.println("生成的 JWT：" + token);

        String userId = validateToken(token);
        System.out.println("解析出的用户ID：" + userId);
    }

    /**
     * 今日作业 2：JWT 完整认证流程图（ASCII 版）
     *
     * 客户端                                  服务端
     *   |                                      |
     *   | 1. 登录请求（用户名+密码）            |
     *   |------------------------------------->|
     *   |                                      |
     *   | 2. 验证用户名密码，生成 JWT（含过期时间） |
     *   |<-------------------------------------|
     *   |                                      |
     *   | 3. 客户端存储 JWT（localStorage/Cookie） |
     *   |                                      |
     *   | 4. 后续请求：Header 携带 Authorization: Bearer <token> |
     *   |------------------------------------->|
     *   |                                      |
     *   | 5. 服务端验证 JWT：                   |
     *   |    a. 解析 Header/Payload             |
     *   |    b. 验证签名（防篡改）              |
     *   |    c. 验证过期时间                   |
     *   |    d. 解析用户信息，生成认证上下文    |
     *   |<-------------------------------------|
     *   |                                      |
     *   | 6. 返回业务数据                      |
     *   v                                      v
     *
     */
}
