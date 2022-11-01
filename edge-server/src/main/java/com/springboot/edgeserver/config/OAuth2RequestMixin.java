/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.springboot.edgeserver.config;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

/**
 * This mixin class is used to serialize/deserialize {@link OAuth2Authentication}.
 *
 * @author Joe Grandja
 * @since 5.3
 * @see OAuth2Authentication
 * @see OAuth2AuthenticationJackson2Module
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonDeserialize(using = OAuth2RequestDeserializer.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
		isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class OAuth2RequestMixin {

	/*@JsonCreator
    OAuth2RequestMixin(@JsonProperty("requestParameters") Map<String, String> requestParameters,
			@JsonProperty("clientId") String clientId,
			@JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities,
			@JsonProperty("approved") boolean approved,
			@JsonProperty("scope") Set<String> scope,
			@JsonProperty("resourceIds") Set<String> resourceIds,
			@JsonProperty("redirectUri") String redirectUri,
			@JsonProperty("responseTypes") Set<String> responseTypes,
			@JsonProperty("extensionProperties") Map<String, Serializable> extensionProperties) {
	}*/

}
