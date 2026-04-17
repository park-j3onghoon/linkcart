import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { UrlInput } from "./UrlInput";

describe("UrlInput", () => {
  it("shows an error for empty input", async () => {
    render(
      <UrlInput
        feedback={null}
        isHydrated
        isWorking={false}
        onSubmit={() => true}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "상품 추가" }));

    expect(await screen.findByText("URL을 입력해주세요")).toBeInTheDocument();
  });

  it("shows an error for invalid urls", async () => {
    render(
      <UrlInput
        feedback={null}
        isHydrated
        isWorking={false}
        onSubmit={() => true}
      />,
    );

    fireEvent.change(screen.getByLabelText("상품 URL"), {
      target: { value: "not-a-url" },
    });
    fireEvent.click(screen.getByRole("button", { name: "상품 추가" }));

    expect(
      await screen.findByText("올바른 URL 형식이 아닙니다 (http:// 또는 https://)"),
    ).toBeInTheDocument();
  });

  it("calls onSubmit with a trimmed valid url", async () => {
    const onSubmit = vi.fn().mockResolvedValue(true);

    render(
      <UrlInput
        feedback={null}
        isHydrated
        isWorking={false}
        onSubmit={onSubmit}
      />,
    );

    fireEvent.change(screen.getByLabelText("상품 URL"), {
      target: { value: " https://example.com/products/1 " },
    });
    fireEvent.click(screen.getByRole("button", { name: "상품 추가" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith("https://example.com/products/1");
    });
  });
});
