import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { useState } from "react";
import { PriorityBadge, SlaBadge, StatusBadge } from "./ui";
import { BarList } from "./Charts";
import { FilePicker } from "./FilePicker";
import { SatisfactionCard } from "./TicketTools";
import type { TicketDetail } from "@/lib/types";

describe("badges never rely on colour alone (AC-DS-*, WCAG 1.4.1)", () => {
  it("print their label", () => {
    render(<><StatusBadge status="WAITING_FOR_CUSTOMER" /><PriorityBadge priority="CRITICAL" /><SlaBadge state="BREACHED" /></>);
    expect(screen.getByText("En attente du client")).toBeInTheDocument();
    expect(screen.getByText(/Critique/)).toBeInTheDocument();
    expect(screen.getByText("Délai dépassé")).toBeInTheDocument();
  });
});

describe("BarList", () => {
  it("prints every value as text and gives each bar an accessible label", () => {
    render(<BarList label="Par catégorie" items={[{ key: "a", label: "Livraison", value: 3 }, { key: "b", label: "Paiement", value: 1 }]} />);
    expect(screen.getByRole("list", { name: "Par catégorie" })).toBeInTheDocument();
    expect(screen.getByLabelText("Livraison : 3 (75 %)")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
  });
});

function PickerHarness({ onError }: { onError: (m: string | null) => void }) {
  const [files, setFiles] = useState<File[]>([]);
  return <FilePicker files={files} onChange={setFiles} onError={onError} />;
}

describe("FilePicker (AC-UX-009)", () => {
  it("shows the approved formats and limits before submission", () => {
    render(<PickerHarness onError={() => {}} />);
    expect(screen.getByText(/JPG, PNG, WEBP ou PDF — 10 Mo max/)).toBeInTheDocument();
  });

  it("accepts a valid file and rejects a forbidden type", async () => {
    const onError = vi.fn();
    const user = userEvent.setup({ applyAccept: false });
    render(<PickerHarness onError={onError} />);
    const input = screen.getByTestId("file-input") as HTMLInputElement;
    await user.upload(input, new File(["x"], "photo.png", { type: "image/png" }));
    expect(screen.getByText("photo.png")).toBeInTheDocument();
    await user.upload(input, new File(["x"], "script.exe", { type: "application/octet-stream" }));
    expect(onError).toHaveBeenLastCalledWith(expect.stringMatching(/Format non autorisé/));
    expect(screen.queryByText("script.exe")).not.toBeInTheDocument();
  });
});

describe("satisfaction (DEC-CS11-05)", () => {
  const ticket = { id: "t1", allowedActions: ["RATE"], satisfaction: null } as unknown as TicketDetail;

  it("asks for a 1–5 rating with labelled choices and sends nothing before a choice", async () => {
    render(<SatisfactionCard ticket={ticket} onRated={async () => {}} />);
    expect(screen.getByRole("button", { name: "Envoyer mon avis" })).toBeDisabled();
    await userEvent.click(screen.getByRole("radio", { name: "4 sur 5" }));
    expect(screen.getByRole("button", { name: "Envoyer mon avis" })).toBeEnabled();
  });

  it("shows the rating once given and nothing when rating is not allowed", () => {
    const { rerender, container } = render(
      <SatisfactionCard ticket={{ ...ticket, satisfaction: { rating: 5, comment: null, ratedAt: "2026-10-30T10:00:00Z" } }} onRated={async () => {}} />);
    expect(screen.getByRole("status")).toHaveTextContent("5/5");
    rerender(<SatisfactionCard ticket={{ ...ticket, allowedActions: [] }} onRated={async () => {}} />);
    expect(container).toBeEmptyDOMElement();
  });
});

describe("custom statuses", () => {
  it("get the tone of their kind and print their code until the vocabulary is loaded", () => {
    render(<StatusBadge status="WAITING_SELLER" kind="ON_HOLD" />);
    const badge = screen.getByText("WAITING_SELLER");
    expect(badge.className).toContain("tone-serious");
  });
});
