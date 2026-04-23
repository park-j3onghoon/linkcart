import { fireEvent, render, screen } from "@testing-library/react";
import type { AuthUser } from "@linkcart/shared";
import { AuthBar } from "./AuthBar";

const user: AuthUser = {
  id: 1,
  email: "ted@example.com",
  display_name: "Teddy",
  avatar_url: null,
  provider: "GOOGLE",
};

describe("AuthBar", () => {
  it("shows a loading placeholder when not hydrated", () => {
    render(
      <AuthBar isHydrated={false} user={null} onLogin={() => {}} onLogout={() => {}} />,
    );
    expect(screen.getByTestId("auth-bar-loading")).toBeInTheDocument();
  });

  it("shows login button when hydrated without user", () => {
    const onLogin = vi.fn();
    render(
      <AuthBar isHydrated user={null} onLogin={onLogin} onLogout={() => {}} />,
    );
    fireEvent.click(screen.getByTestId("login-button"));
    expect(onLogin).toHaveBeenCalled();
  });

  it("shows user badge with display_name when user is present", () => {
    render(
      <AuthBar isHydrated user={user} onLogin={() => {}} onLogout={() => {}} />,
    );
    expect(screen.getByText("Teddy")).toBeInTheDocument();
  });

  it("invokes onLogout when the logout button is clicked", () => {
    const onLogout = vi.fn();
    render(
      <AuthBar isHydrated user={user} onLogin={() => {}} onLogout={onLogout} />,
    );
    fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));
    expect(onLogout).toHaveBeenCalled();
  });

  it("falls back to email initial when avatar and display_name are missing", () => {
    const anonymous: AuthUser = { ...user, display_name: null, avatar_url: null };
    render(
      <AuthBar isHydrated user={anonymous} onLogin={() => {}} onLogout={() => {}} />,
    );
    expect(screen.getByText("T")).toBeInTheDocument();
  });
});
