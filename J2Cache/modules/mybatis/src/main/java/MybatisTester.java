import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;

/**
 * 测试入口
 */
public class MybatisTester {

    public static void main(String[] args) {
        //加载mybatis配置
        String resource = "/mybatis.xml";
        InputStream inputStream = MybatisTester.class.getResourceAsStream(resource);
        //构造sqlSess
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession session = sqlSessionFactory.openSession();
        try {
            System.out.println("mybatis init.");
            /*
            Blog blog = new Blog();
            blog.setId(100);
            blog.setTitle("博客标题1");
            blog.setBody("博客内容1");
            session.insert("new", blog);

            System.out.println("blog inserted");
            */
            for(int i=0;i<10;i++) {
                Blog db = session.selectOne("read", 100);
                System.out.printf("id=%d,title=%s,body=%s%n", db.getId(), db.getTitle(), db.getBody());
                session.commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
            System.exit(0);
        }

    }

}
