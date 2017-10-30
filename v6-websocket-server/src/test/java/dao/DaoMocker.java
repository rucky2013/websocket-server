package dao;

import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO Mocker
 * Created by lemon on 15/6/7.
 */
@ContextConfiguration({"classpath:spring-db.xml", "classpath:spring-dao.xml"})
@Transactional(transactionManager = "transactionManager")
@Rollback
public abstract class DaoMocker extends AbstractTransactionalTestNGSpringContextTests {

}
