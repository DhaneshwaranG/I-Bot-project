import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
} from "react";
import api from "../services/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    localStorage.removeItem("ibot_token");
    localStorage.removeItem("ibot_refresh");
    localStorage.removeItem("ibot_user");
    setUser(null);
  }, []);

  useEffect(() => {
    const stored = localStorage.getItem("ibot_user");
    const token = localStorage.getItem("ibot_token");

    if (stored && token) {
      try {
        setUser(JSON.parse(stored));
      } catch {
        logout();
      }
    }

    setLoading(false);
  }, [logout]);

  const login = useCallback(async (credentials) => {
    const { data } = await api.post("/api/auth/login", credentials);

    const { token, refreshToken, ...userInfo } = data.data;

    localStorage.setItem("ibot_token", token);

    if (refreshToken) {
      localStorage.setItem("ibot_refresh", refreshToken);
    }

    localStorage.setItem("ibot_user", JSON.stringify(userInfo));

    setUser(userInfo);

    return userInfo;
  }, []);

  const register = useCallback(async (userData) => {
    const { data } = await api.post("/api/auth/register", userData);

    const { token, refreshToken, ...userInfo } = data.data;

    localStorage.setItem("ibot_token", token);

    if (refreshToken) {
      localStorage.setItem("ibot_refresh", refreshToken);
    }

    localStorage.setItem("ibot_user", JSON.stringify(userInfo));

    setUser(userInfo);

    return userInfo;
  }, []);

  const updateUser = useCallback(
    (updates) => {
      const updated = { ...user, ...updates };

      localStorage.setItem("ibot_user", JSON.stringify(updated));
      setUser(updated);
    },
    [user],
  );

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        register,
        logout,
        updateUser,
        isAuthenticated: !!user,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
};
