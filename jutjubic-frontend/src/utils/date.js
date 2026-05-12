export function toDate(v) {
  if (!v) return null;

  // ako je ISO string
  if (typeof v === "string" || typeof v === "number") {
    const d = new Date(v);
    return isNaN(d.getTime()) ? null : d;
  }

  // ako je array: [y, m, d, hh, mm, ss, nanos]
  if (Array.isArray(v)) {
    const [y, m, d, hh = 0, mm = 0, ss = 0, nanos = 0] = v;
    const ms = Math.floor(nanos / 1e6);
    const dt = new Date(y, m - 1, d, hh, mm, ss, ms);
    return isNaN(dt.getTime()) ? null : dt;
  }

  return null;
}
