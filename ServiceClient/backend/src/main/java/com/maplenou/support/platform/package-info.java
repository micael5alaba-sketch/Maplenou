/**
 * Technical foundation shared by the adapters of every module: configuration, web error mapping, persistence base
 * class, caching, security (token validation), transactional outbox, request ids, metrics and rate limiting.
 * Never contains business rules and never depends on a business module.
 */
package com.maplenou.support.platform;
