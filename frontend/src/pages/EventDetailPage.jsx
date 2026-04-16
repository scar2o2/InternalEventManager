import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../services/api";

export default function EventDetailPage() {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get(`/events/${id}`)
      .then(({ data }) => setEvent(data))
      .catch((err) => setError(err.response?.data?.message || "Unable to load event."));
  }, [id]);

  if (error) {
    return <p className="error-text">{error}</p>;
  }

  if (!event) {
    return <p>Loading event details...</p>;
  }

  return (
    <section className="detail-panel brutal-card">
      <p className="eyebrow">{event.department}</p>
      <h1>{event.name}</h1>
      <div className="detail-grid">
        <span>Date: {new Date(event.dateTime).toLocaleString()}</span>
        <span>Venue: {event.venue}</span>
        <span>Price: Rs. {event.ticketPrice}</span>
        <span>Available: {event.availableTickets}</span>
      </div>
      <p>This booking experience is limited to authenticated student and faculty users only.</p>
      <Link className="brutal-button" to={`/events/${event.id}/book`}>Continue to Booking</Link>
    </section>
  );
}
