import { useEffect, useState } from "react";
import EventCard from "../components/EventCard";
import { useAuth } from "../context/AuthContext";
import api from "../services/api";

export default function EventListPage() {
  const { auth } = useAuth();
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/events")
      .then(({ data }) => setEvents(data))
      .catch((err) => setError(err.response?.data?.message || "Unable to load events."));
  }, []);

  return (
    <section>
      <div className="section-banner">
        <div>
          <p className="eyebrow">Live Events</p>
          <h1>Browse department happenings</h1>
        </div>
        <div className="stats-panel">
          <span>{events.length} events</span>
          <span>{auth.role === "USER" ? "Dynamic pricing active" : "Admin review mode"}</span>
        </div>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <div className="event-grid">
        {events.map((event) => (
          <EventCard key={event.id} event={event} canBook={auth.role === "USER"} />
        ))}
      </div>
    </section>
  );
}
