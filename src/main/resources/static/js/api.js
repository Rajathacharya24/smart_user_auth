const API_BASE_URL = '/api/v1';

class ApiService {
    constructor() {
        this.tokenKey = 'accessToken';
        this.refreshKey = 'refreshToken';
    }

    setTokens(accessToken, refreshToken) {
        localStorage.setItem(this.tokenKey, accessToken);
        if (refreshToken) {
            localStorage.setItem(this.refreshKey, refreshToken);
        }
    }

    clearTokens() {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.refreshKey);
    }

    getAccessToken() {
        return localStorage.getItem(this.tokenKey);
    }

    getRefreshToken() {
        return localStorage.getItem(this.refreshKey);
    }

    isAuthenticated() {
        return !!this.getAccessToken();
    }

    async request(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getAccessToken();
        if (token && !options.noAuth) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            let response = await fetch(`${API_BASE_URL}${endpoint}`, config);

            // Handle token expiration (401)
            if (response.status === 401 && !options.isRetry && this.getRefreshToken()) {
                const refreshed = await this.refreshToken();
                if (refreshed) {
                    // Retry the original request
                    headers['Authorization'] = `Bearer ${this.getAccessToken()}`;
                    response = await fetch(`${API_BASE_URL}${endpoint}`, {
                        ...config,
                        headers
                    });
                } else {
                    this.clearTokens();
                    window.location.href = '/login.html';
                    throw new Error('Session expired');
                }
            }

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || data.error || 'API request failed');
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    async refreshToken() {
        try {
            const refreshToken = this.getRefreshToken();
            if (!refreshToken) return false;

            const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                this.setTokens(data.accessToken, data.refreshToken);
                return true;
            }
            return false;
        } catch {
            return false;
        }
    }

    async login(email, password) {
        const data = await this.request('/auth/login', {
            method: 'POST',
            noAuth: true,
            body: JSON.stringify({ email, password })
        });
        this.setTokens(data.accessToken, data.refreshToken);
        return data;
    }

    async register(name, email, password) {
        return await this.request('/auth/register', {
            method: 'POST',
            noAuth: true,
            body: JSON.stringify({ name, email, password })
        });
    }

    async logout() {
        const refreshToken = this.getRefreshToken();
        if (refreshToken) {
            try {
                await this.request('/auth/logout', {
                    method: 'POST',
                    body: JSON.stringify({ refreshToken })
                });
            } catch (e) {
                console.warn('Logout request failed, cleaning local state anyway');
            }
        }
        this.clearTokens();
    }

    async getCurrentUser() {
        return await this.request('/users/me');
    }
}

const api = new ApiService();
