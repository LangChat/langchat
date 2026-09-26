export interface LcFileTreeNode {
  children?: LcFileTreeNode[];
  directory: boolean;
  name: string;
  path: string;
  size?: number;
}
