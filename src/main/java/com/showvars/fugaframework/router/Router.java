package com.showvars.fugaframework.router;

import com.showvars.fugaframework.foundation.Context;
import com.showvars.fugaframework.foundation.Response;
import com.showvars.fugaframework.foundation.controllers.DefaultController;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Router {

    private static final Logger log = LogManager.getLogger(Router.class);

    private List<Extension> extensions;

    public void load(File file) {
        try {
            load(new FileInputStream(file));
            log.info("Routes loaded from: {}", file.getPath());
        } catch (Exception ex) {
            log.catching(ex);
        }
    }

    public void load(String path) {
        try {
            load(new FileInputStream(path));
            log.info("Routes loaded from: {}", path);
        } catch (Exception ex) {
            log.catching(ex);
        }
    }

    public void loadFromResources(String path) {
        try {
            load(Router.class.getResourceAsStream(path));
            log.info("Routes loaded from resources: {}", path);
        } catch (Exception ex) {
            log.catching(ex);
        }
    }

    public void load(InputStream input) throws NullPointerException, Exception {
        if (input == null) {
            throw new NullPointerException();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(input));

        RouteMapLoader mapLoader = new RouteMapLoader();

        extensions = mapLoader.load(input);

    }

    public Response forward(Context ctx) {

        Response resp = forward(ctx, extensions);
        if (resp != null) {
            return resp;
        }
        return DefaultController.notFound(ctx);
    }

    private Response forward(Context ctx, List<Extension> exts)  {
        Matcher m;
        for (Extension ext : exts) {
            if (ext.getRequestMethod() != null && ext.getRequestMethod() != ctx.getRequest().getRequestMethod()) {
                continue;
            }
            if (ext.getPattern() == null) {
                m = null;
            } else if (!(m = ext.getPattern().matcher(ctx.getRequest().getPath())).matches()) {
                continue;
            }

            if (ext.getRoute() != null) {
                Route route = ext.getRoute();
                Object[] args = new Object[route.getParameters().size() + 1];
                args[0] = ctx;
                for (int i = 1; i < args.length; i++) {
                    RouteParameter mp = route.getParameters().get(i - 1);
                    if (mp.getType() == RouteParameter.ParameterType.CAPTURE_GROUP) {
                        args[i] = mp.cast(m.group(mp.getCaptureGroup()));
                    } else {
                        args[i] = mp.cast();
                    }
                }
                try {
                    Response resp = (Response) route.getMethod().invoke(null, args);
                    if (resp != null) {
                        return resp;
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    log.catching(ex);
                    Throwable cause = ex.getCause();
                    if (cause == null) {
                        return DefaultController.exception(ctx, ex);
                    } else if (cause instanceof Exception) {
                        return DefaultController.exception(ctx, (Exception) cause);
                    } else {
                        return DefaultController.exception(ctx, ex);
                    }
                }
            } else if (ext.getNodes() != null && !ext.getNodes().isEmpty()) {
                Response resp = (Response) forward(ctx, ext.getNodes());
                if (resp != null) {
                    return resp;
                }
            }
        }
        return null;
    }
}