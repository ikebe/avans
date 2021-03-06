package me.geso.avans;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.geso.avans.annotation.GET;
import me.geso.avans.annotation.POST;
import me.geso.routes.RoutingResult;
import me.geso.routes.WebRouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class Dispatcher {
	private static final Logger logger = LoggerFactory
			.getLogger(Dispatcher.class);
	private final WebRouter<Action> router = new WebRouter<>();

	public Dispatcher() {
	}

	/**
	 * Register classes in the package to dispatcher.
	 * 
	 * @param pkg
	 */
	public void registerPackage(Package pkg) {
		this.registerPackage(pkg.getName());
	}

	/**
	 * Register classes in the package to dispatcher.
	 * 
	 * @param packageName
	 */
	public void registerPackage(String packageName) {
		logger.info("Registering package: {}", packageName);
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		ImmutableSet<ClassInfo> topLevelClasses;
		try {
			topLevelClasses = ClassPath.from(
					contextClassLoader).getTopLevelClasses(packageName);
		} catch (IOException e) {
			// It caused by programming error.
			throw new RuntimeException(e);
		}
		for (ClassInfo classInfo : topLevelClasses) {
			Class<?> klass = classInfo.load();
			if (Controller.class.isAssignableFrom(klass)) {
				Class<? extends Controller> pagesClass = klass
						.asSubclass(Controller.class);
				this.registerClass(pagesClass);
			} else {
				logger.info("{} is not a Controller", klass);
			}
		}
	}

	/**
	 * Register class to dispatcher.
	 * 
	 * @param klass
	 */
	public void registerClass(Class<? extends Controller> klass) {
		try {
			logger.info("Registering class: {}", klass);
			for (Method method : klass.getMethods()) {
				{
					POST post = method.getAnnotation(POST.class);
					if (post != null) {
						String path = post.value();
						Action action = new Action(klass, method);
						logger.info("POST {}", path);
						router.post(path, action);
					}
				}
				{
					GET get = method.getAnnotation(GET.class);
					if (get != null) {
						String path = get.value();
						Action action = new Action(klass, method);
						logger.info("GET {}", path);
						router.get(path, action);
					}
				}
			}
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public void handler(HttpServletRequest request, HttpServletResponse response) {
		String method = request.getMethod();
		String path = request.getPathInfo();
		// log.debug("{} {}", method, path);
		RoutingResult<Action> match = router.match(
				method, path);
		if (match == null) {
			this.writeNotFoundErrorPage(request, response);
			return;
		}

		if (!match.methodAllowed()) {
			this.writeMethodNotAllowedErrorPage(request, response);
			return;
		}

		Map<String, String> captured = match.getCaptured();
		Action action = match.getDestination();
		this.runController(action.getControllerClass(), action.getMethod(),
				request, response, captured);
	}

	// You can replace this method by concrete code in your sub class.
	public void runController(Class<? extends Controller> controllerClass,
			Method method, HttpServletRequest request,
			HttpServletResponse response, Map<String, String> captured) {
		try (Controller controller = controllerClass.newInstance()) {
			controller.invoke(method, request, response, captured);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void writeMethodNotAllowedErrorPage(HttpServletRequest request,
			HttpServletResponse response) {
		response.setCharacterEncoding("UTF-8");
		response.setStatus(405);
		response.setContentType("text/html; charset=utf-8");
		try {
			response.getWriter()
					.write("<!doctype html><html><div style='font-size: 400%'>405 Method Not Allowed</div></html>");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writeNotFoundErrorPage(HttpServletRequest request,
			HttpServletResponse response) {
		response.setCharacterEncoding("UTF-8");
		response.setStatus(404);
		response.setContentType("text/html; charset=utf-8");
		try {
			response.getWriter()
					.write("<!doctype html><html><div style='font-size: 400%'>404 Not Found</div></html>");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public WebRouter<Action> getRouter() {
		return this.router;
	}

	public static class Action {
		public Action(Class<? extends Controller> controllerClass, Method method) {
			this.controllerClass = controllerClass;
			this.method = method;
		}

		public Class<? extends Controller> getControllerClass() {
			return controllerClass;
		}

		public Method getMethod() {
			return method;
		}

		private final Class<? extends Controller> controllerClass;
		private final Method method;
	}

}
