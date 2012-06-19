/**
 * eobjects.org DataCleaner
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.datacleaner.user;

import java.io.Serializable;

import org.eobjects.analyzer.util.StringUtils;
import org.eobjects.datacleaner.util.SecurityUtils;

/**
 * Describes the connection information needed to connect to the DQ monitor
 * application.
 */
public class MonitorConnection implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String _hostname;
    private final int _port;
    private final String _contextPath;
    private final boolean _https;
    private final String _tenantId;
    private final String _username;
    private final String _encodedPassword;

    public MonitorConnection(String hostname, int port, String contextPath, boolean isHttps, String tenantId,
            String username, char[] password) {
        this(hostname, port, contextPath, isHttps, tenantId, username, SecurityUtils.encodePassword(password));
    }

    public MonitorConnection(String hostname, int port, String contextPath, boolean isHttps, String tenantId,
            String username, String encodedPassword) {
        _hostname = hostname;
        _port = port;
        _contextPath = contextPath;
        _https = isHttps;
        _tenantId = tenantId;
        _username = username;
        _encodedPassword = encodedPassword;
    }

    public String getHostname() {
        return _hostname;
    }

    public int getPort() {
        return _port;
    }

    public boolean isHttps() {
        return _https;
    }

    public String getContextPath() {
        return _contextPath;
    }

    public String getEncodedPassword() {
        return _encodedPassword;
    }

    public String getTenantId() {
        return _tenantId;
    }

    public String getUsername() {
        return _username;
    }

    public String getBaseUrl() {
        return "" + (_https ? "https://" : "http://") + _hostname + ":" + _port
                + (StringUtils.isNullOrEmpty(_contextPath) ? "" : "/" + _contextPath);
    }

    public String getRepositoryUrl() {
        return getBaseUrl() + "/repository" + (StringUtils.isNullOrEmpty(_tenantId) ? "" : "/" + _tenantId);
    }

    public boolean isAuthenticationEnabled() {
        return !StringUtils.isNullOrEmpty(_username) && !StringUtils.isNullOrEmpty(_encodedPassword);
    }
}