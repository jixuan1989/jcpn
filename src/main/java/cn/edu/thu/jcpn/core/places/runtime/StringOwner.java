package cn.edu.thu.jcpn.core.places.runtime;

public class StringOwner implements IOwner {

    private String owner;

    public StringOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getName() {
        return this.owner;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StringOwner other = (StringOwner) obj;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return owner;
    }
}
