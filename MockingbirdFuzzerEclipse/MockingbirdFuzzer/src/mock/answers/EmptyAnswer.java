package mock.answers;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author Derrick Lockwood
 * @created 6/7/18.
 */
public class EmptyAnswer implements SubAnswer, RedefineAnswer, StaticAnswer {
    @Override
    public Object handle(Object proxy, Object[] args, Method method) throws Throwable {
        return null;
    }

    @Override
    public Object handle(Object[] args) throws Throwable {
        return null;
    }

    @Override
    public Object handle(Object proxy, Object[] args, Callable<Object> originalMethod, Method method) throws Throwable {
        return null;
    }

    @Override
    public Object handle(Object proxy, Object[] parameters, String name, Class<?> returnType) {
        return null;
    }
}
