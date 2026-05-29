package com.yc.f_six_week.transactionFailure;

@Service
public class TransactionDemoService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. public 方法调用 private 事务方法 → 事务失效
    public void createUserPublic() {
        insertUser();
    }

    // @Transactional 标注在 private 方法上 → Spring 无法代理
    @Transactional(rollbackFor = Exception.class)
    private void insertUser() {
        jdbcTemplate.update("INSERT INTO user (name, age) VALUES (?, ?)", "张三", 18);
        // 模拟异常
        throw new RuntimeException("插入失败，应该回滚");
    }

    // 外部调用这个 public 方法
    public void callInsertUser() {
        // 2. 同类调用 this.insertUser() → 事务失效
        insertUser();
    }

    @Transactional(rollbackFor = Exception.class)
    public void insertUser() {
        jdbcTemplate.update("INSERT INTO user (name, age) VALUES (?, ?)", "李四", 20);
        throw new RuntimeException("插入失败，应该回滚");
    }

    @Transactional(rollbackFor = Exception.class)
    public void insertUserWithCatch() {
        try {
            jdbcTemplate.update("INSERT INTO user (name, age) VALUES (?, ?)", "王五", 22);
            throw new RuntimeException("插入失败，应该回滚");
        } catch (Exception e) {
            // 3. 捕获了异常但没有重新抛出 → 事务不会回滚
            System.out.println("捕获了异常：" + e.getMessage());
        }
    }

}