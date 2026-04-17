import type { ParsePhase } from "../hooks/useProducts";

type ParseProgressProps = {
  phase: ParsePhase;
};

const steps = [
  {
    key: "validating",
    title: "URL 확인",
    description: "형식과 중복 여부를 먼저 점검합니다.",
  },
  {
    key: "parsing",
    title: "상품 정보 파싱",
    description: "백엔드 API가 링크를 해석하고 응답을 정리합니다.",
  },
  {
    key: "success",
    title: "리스트 반영",
    description: "성공 결과를 로컬 저장소와 카드 리스트에 반영합니다.",
  },
] as const;

const stepStyles = {
  active: "border-cyan-300/40 bg-cyan-300/12 text-white",
  complete: "border-emerald-300/40 bg-emerald-300/12 text-white",
  idle: "border-white/10 bg-white/5 text-slate-300",
  error: "border-rose-300/40 bg-rose-300/12 text-white",
};

export function ParseProgress({ phase }: ParseProgressProps) {
  function getStatus(stepKey: (typeof steps)[number]["key"]) {
    if (phase === "error") {
      return stepKey === "parsing" ? "error" : "idle";
    }

    if (phase === "success") {
      return "complete";
    }

    if (phase === stepKey) {
      return "active";
    }

    if (phase === "parsing" && stepKey === "validating") {
      return "complete";
    }

    return "idle";
  }

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium uppercase tracking-[0.28em] text-cyan-300">
          Progress
        </p>
        <p className="text-xs text-slate-400">
          {phase === "idle" && "입력을 기다리는 중"}
          {phase === "validating" && "URL 검사 중"}
          {phase === "parsing" && "파싱 요청 처리 중"}
          {phase === "success" && "추가 완료"}
          {phase === "error" && "다시 시도 필요"}
        </p>
      </div>

      <div className="grid gap-3">
        {steps.map((step, index) => {
          const status = getStatus(step.key);
          return (
            <div
              key={step.key}
              className={`rounded-2xl border px-4 py-3 ${stepStyles[status]}`}
            >
              <div className="flex items-center gap-3">
                <span className="inline-flex h-8 w-8 items-center justify-center rounded-full border border-current/20 text-sm font-semibold">
                  0{index + 1}
                </span>
                <div>
                  <p className="text-sm font-semibold">{step.title}</p>
                  <p className="mt-1 text-sm leading-6 opacity-80">{step.description}</p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
