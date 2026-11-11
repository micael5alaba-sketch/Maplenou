import { buyerPrice, categoryForPrice, commissionRate, fcfa, MONTHLY_FEE } from "./commission";

describe("Maplenou fees (Bakend CommissionService, SellerFeeCategory)", () => {
  it("uses 5 % for Standard and 3 % for High price", () => {
    expect(commissionRate("standard")).toBe(0.05);
    expect(commissionRate("high")).toBe(0.03);
  });

  it("adds the commission to the price the buyer pays; the seller keeps the base price", () => {
    expect(buyerPrice(6_500, "standard")).toEqual({ rate: 0.05, commission: 325, display: 6_825, sellerReceives: 6_500 });
    expect(buyerPrice(25_000, "high")).toEqual({ rate: 0.03, commission: 750, display: 25_750, sellerReceives: 25_000 });
  });

  it("matches the example of the fees document (10 000 FCFA, Standard → 10 500 FCFA)", () => {
    expect(buyerPrice(10_000, "standard")).toEqual({ rate: 0.05, commission: 500, display: 10_500, sellerReceives: 10_000 });
  });

  it("rounds the commission to the whole FCFA per unit, half up (like Java HALF_UP)", () => {
    expect(buyerPrice(510, "standard").commission).toBe(26); // 25.5 → 26
    expect(buyerPrice(1_150, "high").commission).toBe(35); // 34.5 → 35
  });

  it("puts a seller in High price above 10 000 FCFA; monthly fees 500 / 2 000 FCFA", () => {
    expect(categoryForPrice(10_000)).toBe("standard");
    expect(categoryForPrice(10_001)).toBe("high");
    expect(MONTHLY_FEE).toEqual({ standard: 500, high: 2000 });
  });

  it("formats CFA francs without decimals", () => {
    expect(fcfa(11_250, "fr").replace(/\s/g, " ")).toBe("11 250 FCFA");
    expect(fcfa(11_250, "en")).toBe("11,250 FCFA");
  });
});
