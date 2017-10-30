package dao.common;

import com.company.biz.dao.CtCantonInfoDAO;
import com.company.biz.model.CtCantonInfo;
import dao.DaoMocker;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Created by BG246481 on 2016/10/13.
 */
public class CtCantonInfoDAOTest extends DaoMocker {
    @Autowired
    private CtCantonInfoDAO ctCantonInfoDAO;

    @Test
    public void getCantonByName(){
        CtCantonInfo ctCantonInfo = ctCantonInfoDAO.getCantonByName("北京市");

    }

    @Test
    public void getCantonByNameAndType(){
        List<CtCantonInfo> list = ctCantonInfoDAO.selectCantonsByIdUp(10478);
        list.stream().forEach( ct -> {
                logger.info(ct.getId() +"   " + ct.getCantonCode()+"   "+ ct.getCantonName());


        });
    }


}
