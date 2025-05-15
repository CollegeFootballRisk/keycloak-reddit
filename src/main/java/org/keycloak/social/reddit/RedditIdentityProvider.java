/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.social.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.messages.Messages;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import java.util.Set;

/**
 * @author <a href="mailto:mautam@usa.com">Mautam</a>
 */
public class RedditIdentityProvider extends AbstractOAuth2IdentityProvider<RedditIdentityProviderConfig>
        implements SocialIdentityProvider<RedditIdentityProviderConfig> {

    private static final Logger log = Logger.getLogger(RedditIdentityProvider.class);

    public static final String AUTH_URL = "https://www.reddit.com/api/v1/authorize";
    public static final String TOKEN_URL = "https://www.reddit.com/api/v1/access_token";
    public static final String PROFILE_URL = "https://oauth.reddit.com/api/v1/me";
    public static final String GROUP_URL = "https://oauth.reddit.com/api/v1/subreddits/mine/moderator";
    public static final String DEFAULT_SCOPE = "identity";
    public static final String GUILDS_SCOPE = "mysubreddits";

    public RedditIdentityProvider(KeycloakSession session, RedditIdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
        config.setClientAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
    }

    @Override
    protected boolean supportsExternalExchange() {
        return true;
    }

    @Override
    protected String getProfileEndpointForValidation(EventBuilder event) {
        return PROFILE_URL;
    }

    @Override
    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        BrokeredIdentityContext user = new BrokeredIdentityContext(getJsonProperty(profile, "id"), getConfig());

        String username = getJsonProperty(profile, "name");
        String id = getJsonProperty(profile, "id");


        user.setUsername(username);
        //user.setEmail(getJsonProperty(profile, "email"));
        user.setIdp(this);

        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());

        return user;
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        log.debug("doGetFederatedIdentity()");
        JsonNode profile = null;
        try {
            profile = SimpleHttp.doGet(PROFILE_URL, session).header("Authorization", "Bearer " + accessToken).asJson();
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from reddit.", e);
        }

        if (getConfig().hasAllowedGuilds()) {
            if (!isAllowedGuild(accessToken)) {
                throw new ErrorPageException(session, Response.Status.FORBIDDEN, Messages.INVALID_REQUESTER);
            }
        }
        return extractIdentityFromProfile(null, profile);
    }

    protected boolean isAllowedGuild(String accessToken) {
        try {
            JsonNode guilds = SimpleHttp.doGet(GROUP_URL, session).header("Authorization", "Bearer " + accessToken).asJson();
            Set<String> allowedGuilds = getConfig().getAllowedGuildsAsSet();
            for (JsonNode guild : guilds) {
                String guildId = getJsonProperty(guild, "id");
                if (allowedGuilds.contains(guildId)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain subreddits the current user is a moderator of from reddit.", e);
        }
    }

    @Override
    protected String getDefaultScopes() {
        if (getConfig().hasAllowedGuilds()) {
            return DEFAULT_SCOPE + " " + GUILDS_SCOPE;
        } else {
            return DEFAULT_SCOPE;
        }
    }
}
