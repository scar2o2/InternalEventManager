import { createContext, useContext, useEffect, useState } from "react";
import api from "../services/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    const stored = localStorage.getItem("ticket-app-auth");
    return stored ? JSON.parse(stored) : null;
  });

  useEffect(() => {
    if (auth) {
      localStorage.setItem("ticket-app-auth", JSON.stringify(auth));
      localStorage.setItem("ticket-app-token", auth.token);
    } else {
      localStorage.removeItem("ticket-app-auth");
      localStorage.removeItem("ticket-app-token");
    }
  }, [auth]);

  const login = async (payload) => {
    const { data } = await api.post("/auth/login", payload);
    setAuth(data);
    return data;
  };

  const register = async (payload) => {
    const { data } = await api.post("/auth/register", payload);
    setAuth(data);
    return data;
  };

  const logout = () => setAuth(null);

  return (
    <AuthContext.Provider value={{ auth, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
