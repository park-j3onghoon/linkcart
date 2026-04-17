import { fireEvent, render, screen, waitFor } from "@testing-library/react-native";
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

    fireEvent.press(screen.getByRole("button", { name: "상품 추가" }));

    expect(await screen.findByText("URL을 입력해주세요")).toBeTruthy();
  });

  it("calls onSubmit with a trimmed valid url", async () => {
    const onSubmit = jest.fn().mockResolvedValue(true);

    render(
      <UrlInput
        feedback={null}
        isHydrated
        isWorking={false}
        onSubmit={onSubmit}
      />,
    );

    fireEvent.changeText(
      screen.getByPlaceholderText("https://www.coupang.com/vp/products/..."),
      " https://example.com/products/1 ",
    );
    fireEvent.press(screen.getByRole("button", { name: "상품 추가" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith("https://example.com/products/1");
    });
  });
});
