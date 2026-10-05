/** Firestore enum values shared by the backend services. Keep aligned with core/domain. */
export enum ConnectionType {
  DIRECT = "DIRECT",
  GROUP = "GROUP",
}

export enum ConnectionStatus {
  ACTIVE = "ACTIVE",
  CLOSED = "CLOSED",
}

export enum MemberRole {
  OWNER = "OWNER",
  ADMIN = "ADMIN",
  MEMBER = "MEMBER",
}

export enum MemberStatus {
  ACTIVE = "ACTIVE",
  LEFT = "LEFT",
  REMOVED = "REMOVED",
}

export enum PostType {
  PHOTO = "PHOTO",
  VIDEO = "VIDEO",
}

export enum LayoutType {
  SINGLE = "SINGLE",
  GRID = "GRID",
  COLLAGE = "COLLAGE",
  CAROUSEL = "CAROUSEL",
}

export enum PostStatus {
  ACTIVE = "ACTIVE",
  DELETED = "DELETED",
}

export enum MediaType {
  IMAGE = "IMAGE",
  VIDEO = "VIDEO",
}
