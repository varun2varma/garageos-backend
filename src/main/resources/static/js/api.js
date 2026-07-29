window.Api = {

    BASE_URL: "/api/v1",

    async get(url) {

        return this.request(url, {

            method: "GET"

        });

    },

    async post(url, body) {

        return this.request(url, {

            method: "POST",

            body: JSON.stringify(body)

        });

    },

    async put(url, body) {

        return this.request(url, {

            method: "PUT",

            body: JSON.stringify(body)

        });

    },

    async delete(url) {

        return this.request(url, {

            method: "DELETE"

        });

    },

    async request(url, options = {}) {

        const headers = {

            "Content-Type": "application/json",

            ...(options.headers || {})

        };

        // Attach JWT automatically
        if (window.Auth) {

            const token = Auth.getAccessToken();

            if (token) {

                headers.Authorization = `Bearer ${token}`;

            }

        }

        const response = await fetch(

            this.BASE_URL + url,

            {

                ...options,

                headers

            }

        );

        // Unauthorized
//        if (response.status === 401) {
//
//            if (window.Auth) {
//
//                Auth.logout();
//
//            }
//
//            throw new Error("Session expired. Please login again.");
//
//        }

        // Other Errors
        if (!response.ok) {

            let message = "Something went wrong.";

            try {

                const error = await response.json();

                message =
                    error.message ??
                    error.error ??
                    error.data?.message ??
                    message;

            } catch (e) {

                message = await response.text();

            }

            const err = new Error(message);

            err.status = response.status;

            throw err;

        }

        // No Content
        if (response.status === 204) {

            return null;

        }

        // Some DELETE APIs may return empty body
        const text = await response.text();

        if (!text) {

            return null;

        }

        const result = JSON.parse(text);

        // Keep GarageOS API wrapper behavior
        return result.data ?? result;

    }

};