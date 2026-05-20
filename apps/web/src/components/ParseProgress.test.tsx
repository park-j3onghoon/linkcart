import { render, screen } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { ParseProgress } from "./ParseProgress";

describe("ParseProgress", () => {
  it("idle phase에서는 입력 대기 안내", () => {
    render(<ParseProgress phase="idle" />);

    expect(screen.getByText("입력을 기다리는 중")).toBeInTheDocument();
  });

  it("validating phase에서 URL 검사 중 표시", () => {
    render(<ParseProgress phase="validating" />);

    expect(screen.getByText("URL 검사 중")).toBeInTheDocument();
  });

  it("parsing phase에서 파싱 요청 처리 중 표시", () => {
    render(<ParseProgress phase="parsing" />);

    expect(screen.getByText("파싱 요청 처리 중")).toBeInTheDocument();
  });

  it("success phase에서 추가 완료 표시", () => {
    render(<ParseProgress phase="success" />);

    expect(screen.getByText("추가 완료")).toBeInTheDocument();
  });

  it("error phase에서 재시도 안내 표시", () => {
    render(<ParseProgress phase="error" />);

    expect(screen.getByText("다시 시도 필요")).toBeInTheDocument();
  });

  it("steps의 3개 단계가 항상 렌더된다", () => {
    render(<ParseProgress phase="idle" />);

    expect(screen.getByText("URL 확인")).toBeInTheDocument();
    expect(screen.getByText("상품 정보 파싱")).toBeInTheDocument();
    expect(screen.getByText("리스트 반영")).toBeInTheDocument();
  });
});
