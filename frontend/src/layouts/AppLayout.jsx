import { useEffect, useMemo, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

export default function AppLayout() {
  const { auth, logout } = useAuth();
  const { showToast } = useToast();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [panelOpen, setPanelOpen] = useState(false);
  const [assistantOpen, setAssistantOpen] = useState(false);
  const [assistantQuestion, setAssistantQuestion] = useState("");
  const [assistantReply, setAssistantReply] = useState("");
  const [assistantLoading, setAssistantLoading] = useState(false);

  const visibleNotifications = useMemo(() => notifications.slice(0, 6), [notifications]);

  const loadNotifications = async () => {
    try {
      const [notificationResponse, unreadResponse] = await Promise.all([
        api.get("/notifications"),
        api.get("/notifications/unread-count")
      ]);
      setNotifications(notificationResponse.data);
      setUnreadCount(unreadResponse.data.count);
    } catch {
      return undefined;
    }
    return undefined;
  };

  useEffect(() => {
    loadNotifications();
    const intervalId = window.setInterval(loadNotifications, 15000);
    return () => window.clearInterval(intervalId);
  }, []);

  const markAsRead = async (notificationId) => {
    try {
      await api.patch(`/notifications/${notificationId}/read`, {});
      await loadNotifications();
    } catch (err) {
      showToast({
        title: "Notification update failed",
        message: err.response?.data?.message || "Could not mark this notification as read.",
        variant: "error"
      });
    }
  };

  const markAllAsRead = async () => {
    try {
      await api.patch("/notifications/read-all", {});
      await loadNotifications();
    } catch (err) {
      showToast({
        title: "Notification update failed",
        message: err.response?.data?.message || "Could not mark all notifications as read.",
        variant: "error"
      });
    }
  };

  const askAssistant = async (submitEvent) => {
    submitEvent.preventDefault();
    if (!assistantQuestion.trim()) {
      return;
    }
    try {
      setAssistantLoading(true);
      const { data } = await api.post("/chat/event-assistant", { message: assistantQuestion });
      setAssistantReply(data.reply);
    } catch (err) {
      setAssistantReply(err.response?.data?.message || "Unable to answer that question right now.");
    } finally {
      setAssistantLoading(false);
    }
  };

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
          <div className="notification-shell">
            <button className="notification-bell" type="button" onClick={() => setPanelOpen((current) => !current)}>
              Notifications
              {unreadCount ? <span className="notification-count">{unreadCount}</span> : null}
            </button>
            {panelOpen ? (
              <div className="notification-panel brutal-card">
                <div className="notification-header">
                  <strong>Updates</strong>
                  <button type="button" className="text-button" onClick={markAllAsRead}>Mark all read</button>
                </div>
                <div className="notification-list">
                  {visibleNotifications.length ? visibleNotifications.map((notification) => (
                    <article
                      key={notification.id}
                      className={`notification-item ${notification.readStatus ? "read" : "unread"}`}
                    >
                      <p>{notification.message}</p>
                      <small>{new Date(notification.timestamp).toLocaleString()}</small>
                      {!notification.readStatus ? (
                        <button type="button" className="text-button" onClick={() => markAsRead(notification.id)}>
                          Mark as read
                        </button>
                      ) : null}
                    </article>
                  )) : <p className="muted-text">No notifications yet.</p>}
                </div>
              </div>
            ) : null}
          </div>
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
      <div className={`assistant-widget ${assistantOpen ? "open" : ""}`}>
        {assistantOpen ? (
          <div className="assistant-panel brutal-card">
            <div className="assistant-header">
              <strong>Event Assistant</strong>
              <button type="button" className="text-button" onClick={() => setAssistantOpen(false)}>Close</button>
            </div>
            <p className="muted-text">Ask about events, timings, venues, or ticket availability.</p>
            <form className="assistant-form" onSubmit={askAssistant}>
              <input
                value={assistantQuestion}
                onChange={(event) => setAssistantQuestion(event.target.value)}
                placeholder="What events are available today?"
              />
              <button className="brutal-button secondary" type="submit" disabled={assistantLoading}>
                {assistantLoading ? "Thinking..." : "Ask"}
              </button>
            </form>
            {assistantReply ? <p className="assistant-reply">{assistantReply}</p> : null}
          </div>
        ) : null}
        <button
          type="button"
          className="assistant-launcher"
          onClick={() => setAssistantOpen((current) => !current)}
        >
          Ask Events
        </button>
      </div>
    </div>
  );
}
