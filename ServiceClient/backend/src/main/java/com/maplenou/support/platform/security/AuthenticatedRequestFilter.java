package com.maplenou.support.platform.security;

import jakarta.servlet.Filter;

/**
 * A filter a module contributes to the security chain; it runs right after the access token has been validated
 * (for example, identity rejects users deactivated by an administrator). Declare it as a bean; it is not registered
 * as a plain servlet filter.
 */
public interface AuthenticatedRequestFilter extends Filter {
}
