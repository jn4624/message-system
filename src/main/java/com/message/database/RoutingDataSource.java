package com.message.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class RoutingDataSource extends AbstractRoutingDataSource {

	private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

	/*
	  커넥션을 얻기 전에 호출됨
	  - 어떤 데이터소스에서 커넥션을 얻어
	    어느 데이터베이스로 갈 것인지 찾는 과정에서 호출되는 메서드
	  - 해당 메서드가 어떤 값을 리턴하느냐에 따라
	    어떤 데이터베이스로 갈지가 결정됨
	 */
	@Override
	protected Object determineCurrentLookupKey() {
		String dataSourceKey = TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "replica" : "source";
		log.info("Routing to {} datasource", dataSourceKey);
		return dataSourceKey;
	}
}
