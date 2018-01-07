package com.spend.engine.pool;

import java.io.Serializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 线程池执行的任务
 */

public class ThreadPool implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger logger = LogManager.getLogger(ThreadPool.class.getName());

	private Thread threadPool;

	private Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		return t;
	}

	public ThreadPool(Thread thread) {
		this.threadPool = thread;
	}

	public ThreadPool(Runnable runnable) {
		this.threadPool = newThread(runnable);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		threadPool.start();
		//threadPool.run();
		logger.info("Thread " + threadPool.getName() + " start!");
	}

}