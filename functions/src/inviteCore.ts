import {createHash, randomInt} from "node:crypto";

export const DIRECT_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
export const DIRECT_CODE_LENGTH = 8;

export function normalizeInviteCode(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const normalized = value.trim().replaceAll("-", "").toUpperCase();
  if (normalized.length !== DIRECT_CODE_LENGTH) return null;
  for (const character of normalized) {
    if (!DIRECT_CODE_ALPHABET.includes(character)) return null;
  }
  return normalized;
}

export function generateInviteCode(): string {
  let result = "";
  for (let index = 0; index < DIRECT_CODE_LENGTH; index += 1) {
    result += DIRECT_CODE_ALPHABET[randomInt(DIRECT_CODE_ALPHABET.length)];
  }
  return result;
}

export function formatInviteCode(normalized: string): string {
  return `${normalized.slice(0, 4)}-${normalized.slice(4)}`;
}

export function sha256(value: string): string {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

export function directKeyFor(firstUid: string, secondUid: string): string {
  return sha256(JSON.stringify([firstUid, secondUid].sort()));
}
