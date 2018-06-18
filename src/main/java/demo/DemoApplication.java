package demo;

import demo.blog.Blog;
import demo.blog.Post;
import demo.blog.PostRepository;
import demo.crm.Crm;
import demo.crm.Order;
import demo.crm.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import xdb.jpa.DataSourceRegistration;
import xdb.jpa.EnableMultipleJpaRegistrations;
import xdb.jpa.JpaRegistration;
import xdb.jpa.JpaRegistrationConfigurer;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@EnableMultipleJpaRegistrations({
	@JpaRegistration(prefix = "crm", rootPackageClass = Order.class),
	@JpaRegistration(prefix = "blog", rootPackageClass = Post.class),
})
@Log4j2
public class DemoApplication {

		public static void main(String args[]) {
				SpringApplication.run(DemoApplication.class, args);
		}

		/**
			* Comment out the `@Component` annotation and it'll instead configure the `DataSource` for you
			* based on Spring Boot-style configuration properties. Make sure to re-instate the MySQL dependency in `pom.xml` and
			* that you have the appropriate DDL in your local database.
			*/
		@Component
		public static class MyConfiguration implements JpaRegistrationConfigurer {

				private final Map<String, DataSource> ds = new ConcurrentHashMap<>();

				public MyConfiguration() {
						this.ds.put("blog", ds("blog"));
						this.ds.put("crm", ds("crm"));
				}

				private static DataSource ds(String name) {
						return new EmbeddedDatabaseBuilder()
							.setType(EmbeddedDatabaseType.H2)
							.setName(name)
							.addScript(name + "-schema.sql")
							.build();
				}

				@Override
				public DataSource getDataSourceFor(String dataSourceName) {
						return this.ds.get(dataSourceName);
				}
		}

		private static class LoggingRunner implements ApplicationRunner {

				private final DataSourceRegistration dsr;
				private final DataSource ds;
				private final JdbcTemplate jt;
				private final EntityManagerFactory emf;
				private final JpaTransactionManager jtaTxManager;
				private final Runnable runnable;
				private final String label;

				LoggingRunner(String label, DataSourceRegistration dsr, DataSource ds,
																		JdbcTemplate jt, EntityManagerFactory emf, JpaTransactionManager jtaTxManager,
																		Runnable runnable) {
						this.dsr = dsr;
						this.ds = ds;
						this.label = label;
						this.jt = jt;
						this.emf = emf;
						this.jtaTxManager = jtaTxManager;
						this.runnable = runnable;
				}

				@Override
				public void run(ApplicationArguments args) throws Exception {
						log.info("======================================================================");
						log.info(this.label.toUpperCase() + ':');
						log.info(ToStringBuilder.reflectionToString(dsr));
						log.info(ToStringBuilder.reflectionToString(ds));
						log.info(ToStringBuilder.reflectionToString(jt));
						log.info(ToStringBuilder.reflectionToString(emf));
						log.info(ToStringBuilder.reflectionToString(jtaTxManager));
						runnable.run();
						log.info(System.lineSeparator());
				}
		}


		@Bean
		ApplicationRunner crmRunner(
			@Crm DataSourceRegistration crmDSR,
			@Crm DataSource crmDS,
			@Crm JdbcTemplate crmJT,
			@Crm EntityManagerFactory crmEMF,
			@Crm JpaTransactionManager crmTxManager,
			@Crm TransactionTemplate transactionTemplate,
			OrderRepository or) {
				return new LoggingRunner("crm", crmDSR, crmDS, crmJT, crmEMF, crmTxManager, () -> {

						transactionTemplate
							.execute((TransactionCallback<Object>) transactionStatus ->
								or.saveAll(Arrays.asList(new Order(null, "429y2"), new Order(null, "342371s"))));

						crmEMF
							.createEntityManager()
							.createQuery("select o from " + Order.class.getName() + " o", Order.class)
							.getResultList()
							.forEach(p -> log.info("order: " + ToStringBuilder.reflectionToString(p)));
						or.findAll().forEach(o -> log.info("order (JPA): " + ToStringBuilder.reflectionToString(o)));
				});
		}

		@Bean
		ApplicationRunner blogRunner(
			@Blog DataSourceRegistration blogDSR,
			@Blog DataSource blogDS,
			@Blog JdbcTemplate blogJT,
			@Blog EntityManagerFactory blogEMF,
			@Blog JpaTransactionManager blogTxManager,
			@Blog TransactionTemplate transactionTemplate,
			PostRepository pr) {

				return new LoggingRunner("blog", blogDSR, blogDS, blogJT, blogEMF, blogTxManager, () -> {

						transactionTemplate
							.execute((TransactionCallback<Object>) transactionStatus ->
								pr.saveAll(Arrays.asList(new Post(null, "A New Hope"), new Post(null, "You Won't Beleive This Listacle Gone Wild"))));

						blogEMF
							.createEntityManager()
							.createQuery("select p from " + Post.class.getName() + " p", Post.class)
							.getResultList()
							.forEach(p -> log.info("post: " + ToStringBuilder.reflectionToString(p)));

						pr.findAll().forEach(p -> log.info("post (JPA): " + ToStringBuilder.reflectionToString(p)));

				});
		}
}

