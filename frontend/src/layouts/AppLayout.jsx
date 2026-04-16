import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function AppLayout() {
  const { auth, logout } = useAuth();

  return (
    <div className="shell">
      <header className="topbar">
        <Link className="brand" to={auth.role === "ADMIN" ? "/admin/dashboard" : "/events"}>
          DeptTix
        </Link>
        <nav className="nav-links">
          {auth.role === "USER" ? (
            <>
              <NavLink to="/events">Events</NavLink>
              <NavLink to="/my-bookings">My Bookings</NavLink>
            </>
          ) : (
            <>
              <NavLink to="/admin/dashboard">Dashboard</NavLink>
              <NavLink to="/admin/events">Manage Events</NavLink>
            </>
          )}
        </nav>
        <div className="user-strip">
          <span>{auth.name}</span>
          <span className="role-chip">{auth.role}</span>
          <button className="brutal-button small" onClick={logout}>
            Logout
          </button>
        </div>
      </header>
      <main className="page-wrap">
        <Outlet />
      </main>
    </div>
  );
}
