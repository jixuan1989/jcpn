package cn.edu.thu.jcpn.common;

/**
 * notice: if this.left.equals(other.left) && this.right.equals(other.right), the equals function will return true.
 * Therefore, BE CAREFUL if you use it as the key in a Map structure.
 *
 * @param <L>
 * @param <R>
 * @author hxd
 */
public class Triple<L, M, R> {

    private L left;
    private M middle;
    private R right;

    public Triple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public M getMiddle() {
        return middle;
    }

    public void setMiddle(M middle) {
        this.middle = middle;
    }

    public R getRight() {
        return right;
    }

    public void setRight(R right) {
        this.right = right;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        result = prime * result + ((middle == null) ? 0 : middle.hashCode());
        result = prime * result + ((right == null) ? 0 : right.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Triple other = (Triple) obj;
        if (left == null) {
            if (other.left != null)
                return false;
        }
        else if (!left.equals(other.left)) {
            return false;
        }

        if (middle == null) {
            if (other.middle != null)
                return false;
        }
        else if (!middle.equals(other.middle)) {
            return false;
        }

        if (right == null) {
            if (other.right != null)
                return false;
        } else if (!right.equals(other.right)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Pair [left=" + left + ", middle=" + middle + ", right=" + right + "]";
    }

}
