/*
==========================================================
 GarageOS Authentication Manager
==========================================================
*/

window.Auth = {

    ACCESS_TOKEN: "garage_access_token",

    REFRESH_TOKEN: "garage_refresh_token",

    USER: "garage_user",

    EXPIRES_AT: "garage_expires_at",

    saveSession(response) {

        localStorage.setItem(
            this.ACCESS_TOKEN,
            response.accessToken
        );

        localStorage.setItem(
            this.REFRESH_TOKEN,
            response.refreshToken
        );

        if (response.user) {

            localStorage.setItem(
                this.USER,
                JSON.stringify(response.user)
            );

        }

        if (response.expiresIn) {

            const expiresAt =
                Date.now() + (response.expiresIn * 1000);

            localStorage.setItem(
                this.EXPIRES_AT,
                expiresAt
            );

        }

    },

    getAccessToken() {

        return localStorage.getItem(
            this.ACCESS_TOKEN
        );

    },

    getRefreshToken() {

        return localStorage.getItem(
            this.REFRESH_TOKEN
        );

    },

    getUser() {

        const user =
            localStorage.getItem(this.USER);

        return user
            ? JSON.parse(user)
            : null;

    },

    isLoggedIn() {

        const token =
            this.getAccessToken();

        if (!token) {

            return false;

        }

        const expiresAt =
            localStorage.getItem(
                this.EXPIRES_AT
            );

        if (!expiresAt) {

            return true;

        }

        return Date.now() < Number(expiresAt);

    },

    clearSession() {

        localStorage.removeItem(this.ACCESS_TOKEN);

        localStorage.removeItem(this.REFRESH_TOKEN);

        localStorage.removeItem(this.USER);

        localStorage.removeItem(this.EXPIRES_AT);

    },

    async logout() {

        try {

            await Api.post("/auth/logout", {

                refreshToken: this.getRefreshToken()

            });

        }
        catch (e) {

            console.warn("Logout API failed.", e);

        }

        this.clearSession();

        window.location.replace("/auth/login.html");

    },

    requireLogin() {

        if (!this.isLoggedIn()) {

            window.location.replace("/auth/login.html");

            return false;

        }

        return true;

    },

    async refreshAccessToken() {

        const refreshToken =
            this.getRefreshToken();

        if (!refreshToken) {

            this.logout();

            return;

        }

        try {

            const response =
                await fetch(
                    "/api/v1/auth/refresh",
                    {

                        method: "POST",

                        headers: {

                            "Content-Type":
                                "application/json"

                        },

                        body: JSON.stringify({

                            refreshToken

                        })

                    });

            if (!response.ok) {

                this.logout();

                return;

            }

            const data =
                await response.json();

            this.saveSession(data);

        }
        catch (e) {

            console.error(e);

            this.logout();

        }

    }

};