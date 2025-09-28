import * as Sentry from "@sentry/vue";
import { BrowserTracing } from "@sentry/tracing";

/**
 * Initialize Sentry for Vue.js application
 * Provides error tracking, performance monitoring, and user context
 */
export function initSentry(app, router) {
  // Only initialize if DSN is provided
  const dsn = process.env.VUE_APP_SENTRY_DSN;
  if (!dsn) {
    console.log('🔍 Sentry DSN not configured - error monitoring disabled');
    return;
  }

  try {
    Sentry.init({
      app,
      dsn,
      
      // Environment and release tracking
      environment: process.env.NODE_ENV || 'development',
      release: process.env.VUE_APP_VERSION || 'development',
      
      // Integrations
      integrations: [
        new BrowserTracing({
          routingInstrumentation: Sentry.vueRouterInstrumentation(router),
          tracePropagationTargets: [
            "localhost", 
            /^\//,  // relative URLs
            /^https:\/\/linhnh\.site/,  // your domain
          ],
        }),
        new Sentry.Replay({
          // Capture 10% of all sessions,
          // plus 100% of sessions with an error
          sessionSampleRate: 0.1,
          errorSampleRate: 1.0,
        }),
      ],

      // Performance monitoring
      tracesSampleRate: parseFloat(process.env.VUE_APP_SENTRY_TRACES_SAMPLE_RATE || '0.1'),
      
      // Capture console errors
      captureUnhandledRejections: true,
      
      // Filter out noise
      beforeSend(event, hint) {
        // Filter out development-only errors
        if (process.env.NODE_ENV === 'development') {
          // Skip certain development errors
          if (event.exception && event.exception.values) {
            const error = event.exception.values[0];
            if (error.type === 'ChunkLoadError' || 
                error.value?.includes('Loading chunk')) {
              return null; // Don't send chunk load errors in development
            }
          }
        }

        // Filter out bot traffic
        const userAgent = event.request?.headers?.['User-Agent'];
        if (userAgent && /bot|crawler|spider|crawling/i.test(userAgent)) {
          return null;
        }

        // Add additional context
        if (hint.originalException) {
          event.extra = {
            ...event.extra,
            vue_info: {
              router_path: router?.currentRoute?.value?.path,
              router_name: router?.currentRoute?.value?.name,
            }
          };
        }

        return event;
      },

      // Don't send personal data
      sendDefaultPii: false,
      
      // Maximum breadcrumbs to keep
      maxBreadcrumbs: 50,
    });

    console.log('✅ Sentry initialized successfully');
    
    // Add global error handler for unhandled Vue errors
    app.config.errorHandler = (error, instance, info) => {
      console.error('Vue Error:', error, info);
      
      Sentry.withScope(scope => {
        scope.setTag('error_boundary', 'vue');
        scope.setContext('vue_info', {
          component_name: instance?.$options?.name || 'Unknown',
          error_info: info,
          props: instance?.$props,
        });
        Sentry.captureException(error);
      });
    };

  } catch (error) {
    console.error('Failed to initialize Sentry:', error);
  }
}

/**
 * Set user context for Sentry
 */
export function setSentryUser(user) {
  Sentry.setUser({
    id: user.id,
    email: user.email,
    username: user.name || user.username,
  });
}

/**
 * Clear user context (on logout)
 */
export function clearSentryUser() {
  Sentry.setUser(null);
}

/**
 * Add breadcrumb for user actions
 */
export function addBreadcrumb(message, category = 'user', level = 'info', data = {}) {
  Sentry.addBreadcrumb({
    message,
    category,
    level,
    data,
    timestamp: Date.now() / 1000,
  });
}

/**
 * Capture custom exception with additional context
 */
export function captureException(error, context = {}) {
  Sentry.withScope(scope => {
    // Add custom context
    Object.keys(context).forEach(key => {
      scope.setExtra(key, context[key]);
    });
    
    scope.setTag('custom_capture', true);
    Sentry.captureException(error);
  });
}

/**
 * Capture custom message
 */
export function captureMessage(message, level = 'info', context = {}) {
  Sentry.withScope(scope => {
    scope.setLevel(level);
    
    // Add custom context
    Object.keys(context).forEach(key => {
      scope.setExtra(key, context[key]);
    });
    
    Sentry.captureMessage(message);
  });
}

/**
 * Start a transaction for performance monitoring
 */
export function startTransaction(name, op = 'navigation') {
  return Sentry.startTransaction({
    name,
    op,
  });
}

/**
 * Add tag to current scope
 */
export function setTag(key, value) {
  Sentry.setTag(key, value);
}

/**
 * Add context to current scope
 */
export function setContext(key, context) {
  Sentry.setContext(key, context);
}

// Export Sentry for direct access if needed
export { Sentry };