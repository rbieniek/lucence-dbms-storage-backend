package de.bieniekconsulting.logstore.lucene.jdbc.directory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class JdbcDirectoryFactoryBean implements FactoryBean<JdbcDirectory>, InitializingBean, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private JdbcFileManager fileManager;
	private JdbcFileRegistry fileRegistry;
	private JdbcLockFactory lockFactory;

	@Override
	public JdbcDirectory getObject() throws Exception {
		return new JdbcDirectory(fileManager, fileRegistry, lockFactory);
	}

	@Override
	public Class<?> getObjectType() {
		return JdbcDirectory.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.fileManager = applicationContext.getBean(JdbcFileManager.class);
		this.lockFactory = applicationContext.getBean(JdbcLockFactory.class);
		this.fileRegistry = applicationContext.getBean(JdbcFileRegistry.class);
	}

}
