package com.qibaike.thriftnameserver.command.ping;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;

import com.jcabi.aspects.Loggable;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.qibaike.thriftnameserver.rpc.PoolAble;
import com.qibaike.thriftnameserver.rpc.TSNode;

public class ThriftPingCommand extends HystrixCommand<Integer> {
	private static final HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory
			.asKey("ThriftPingGroup");
	private static final HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey("ping");
	private static final HystrixThreadPoolKey threadPoolKey = HystrixThreadPoolKey.Factory
			.asKey("T-ThriftPingGroup");

	private static final HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties
			.Setter().withExecutionTimeoutInMilliseconds(5000).withFallbackEnabled(true);
	private static final HystrixThreadPoolProperties.Setter threadPoolProperties = HystrixThreadPoolProperties
			.Setter().withCoreSize(4).withMaxQueueSize(10);
	private static final HystrixCommand.Setter setter = HystrixCommand.Setter
			.withGroupKey(groupKey).andCommandKey(commandKey).andThreadPoolKey(threadPoolKey)
			.andCommandPropertiesDefaults(commandProperties)
			.andThreadPoolPropertiesDefaults(threadPoolProperties);

	private final TSNode tsnode;

	public ThriftPingCommand(TSNode tsnode) {
		super(setter);
		this.tsnode = tsnode;
	}

	@Loggable
	public int ping(TSNode tsnode) {
		return this.execute();
	}

	public int ping() {
		return this.ping(this.tsnode);
	}

	@Override
	protected Integer run() throws Exception {
		String host = this.tsnode.getHost();
		int port = this.tsnode.getPort();

		TSocket transport = new TSocket(host, port, 1000);
		TProtocol protocol = new TBinaryProtocol(transport);
		PoolAble.Client client = new PoolAble.Client(protocol);
		transport.open();
		int vNodes = -1;
		try {
			vNodes = client.ping();
		} finally {
			if (transport.isOpen()) {
				transport.close();
			}
		}
		return vNodes;
	}

	@Override
	protected Integer getFallback() {
		return this.getFallback(this.tsnode);
	}

	@Loggable(value = Loggable.WARN)
	protected Integer getFallback(TSNode tsnode) {
		return -1;
	}
}
