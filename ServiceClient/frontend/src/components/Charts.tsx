"use client";

// Single-series charts in one validated hue (--chart-1). Values are always printed as text and each mark
// has a hover/focus tooltip; the list itself is the accessible table view (dataviz method).

import { useFormat, useMessages } from "@/lib/i18n/client";
import { format } from "@/lib/i18n/define";
import { charts } from "@/lib/i18n/messages/charts";

export function BarList({
  items,
  label,
}: {
  items: { key: string; label: string; value: number }[];
  label: string;
}) {
  const t = useMessages(charts);
  const f = useFormat();
  const max = Math.max(1, ...items.map((i) => i.value));
  const total = items.reduce((s, i) => s + i.value, 0);
  return (
    <ul className="barlist" aria-label={label}>
      {items.map((i) => {
        const pct = total ? Math.round((i.value / total) * 100) : 0;
        const vars = { label: i.label, value: f.number(i.value), pct: f.number(pct) };
        return (
          <li key={i.key}>
            <span className="secondary">{i.label}</span>
            <span className="bar-hit" tabIndex={0} aria-label={format(t.barLabel, vars)}>
              <span className="bar-track">
                <span className="bar-fill" style={{ width: `${(i.value / max) * 100}%`, display: "block" }} />
              </span>
              <span className="chart-tip" aria-hidden="true">
                {format(t.barTip, vars)}
              </span>
            </span>
            <span className="bar-value">{i.value}</span>
          </li>
        );
      })}
    </ul>
  );
}

export function ColumnChart({ points, label }: { points: { key: string; label: string; value: number }[]; label: string }) {
  const t = useMessages(charts);
  const f = useFormat();
  const max = Math.max(1, ...points.map((p) => p.value));
  return (
    <figure style={{ margin: 0 }}>
      <div className="columns" role="list" aria-label={label}>
        {points.map((p) => (
          <div key={p.key} className="col-hit" role="listitem" tabIndex={0} aria-label={format(t.columnLabel, { label: p.label, value: f.number(p.value) })}>
            <div className="col-fill" style={{ height: `${(p.value / max) * 100}%` }} />
            <span className="chart-tip" aria-hidden="true">
              {format(t.columnTip, { label: p.label, value: f.number(p.value) })}
            </span>
          </div>
        ))}
      </div>
      {points.length > 0 && (
        <div className="axis-row" aria-hidden="true">
          <span>{points[0].label}</span>
          <span>{format(t.axisMax, { max: f.number(max) })}</span>
          <span>{points[points.length - 1].label}</span>
        </div>
      )}
    </figure>
  );
}
