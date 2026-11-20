package com.ascent.ui;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

/**
 * 把耗时的网络请求放到后台线程，结果回到 Swing 事件分发线程（EDT）处理。
 *
 * <p>
 * Swing 的界面组件只能在 EDT 里更新，而 Socket 读取会阻塞；如果直接在按钮回调里
 * 发请求，界面会假死。这个类的用法：
 * </p>
 *
 * <pre>
 * AsyncRunner.run(
 *     // 后台：真正做网络请求
 *     () -&gt; client.getCategories(),
 *     // 成功：回到 EDT，拿到结果
 *     categories -&gt; rebuildComboBox(categories),
 *     // 失败：回到 EDT，提示用户
 *     error -&gt; showError(error),
 *     // 收尾：无论成败都要恢复按钮状态
 *     () -&gt; loginButton.setEnabled(true));
 * </pre>
 *
 * <p>
 * 所有回调都在 EDT 上执行，可以直接操作组件。
 * </p>
 *
 * @author ascent
 * @version 1.1
 */
public final class AsyncRunner {

	/**
	 * 后台任务。
	 *
	 * @param <T> 任务返回的结果类型
	 */
	public interface Task<T> {
		/**
		 * 在后台线程执行。
		 *
		 * @return 任务结果
		 * @throws Exception 任务失败时抛出
		 */
		T run() throws Exception;
	}

	/**
	 * 成功回调。
	 *
	 * @param <T> 结果类型
	 */
	public interface Success<T> {
		/**
		 * 在 EDT 上执行。
		 *
		 * @param result 后台任务的结果
		 */
		void run(T result);
	}

	/**
	 * 失败回调。
	 */
	public interface Failure {
		/**
		 * 在 EDT 上执行。
		 *
		 * @param error 后台任务抛出的异常
		 */
		void run(Exception error);
	}

	/** 收尾回调，成功和失败都会执行一次。 */
	public interface Finally {
		/** 在 EDT 上执行。 */
		void run();
	}

	/**
	 * 工具类不允许实例化。
	 */
	private AsyncRunner() {
	}

	/**
	 * 提交一个后台任务。
	 *
	 * @param task    后台执行的任务
	 * @param success 成功回调，可以为 null
	 * @param failure 失败回调，可以为 null
	 * @param last    收尾回调，可以为 null
	 * @param <T>     结果类型
	 */
	public static <T> void run(Task<T> task, Success<T> success, Failure failure,
			Finally last) {
		new SwingWorker<T, Void>() {

			@Override
			protected T doInBackground() throws Exception {
				return task.run();
			}

			@Override
			protected void done() {
				try {
					T result = get();
					if (success != null) {
						success.run(result);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					if (failure != null) {
						failure.run(new Exception("操作被中断", e));
					}
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					Exception reported = cause instanceof Exception
							? (Exception) cause
							: new Exception(cause);
					if (failure != null) {
						failure.run(reported);
					}
				} finally {
					if (last != null) {
						last.run();
					}
				}
			}
		}.execute();
	}
}
