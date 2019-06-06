package de.tum.in.net.ixy.memory;

import de.tum.in.net.ixy.generic.IxyDmaMemory;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * A simple implementation of Ixy's direct memory addresses specification.
 * <p>
 * All the implementation of the class is automatically generated using Lombok.
 *
 * @author Esaú García Sánchez-Torija
 */
@ToString(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, doNotUseGetters = true)
@SuppressWarnings({"HardCodedStringLiteral", "ClassWithoutNoArgConstructor"})
public final class DmaMemory implements IxyDmaMemory {

	/**
	 * -------------- GETTER --------------
	 * Returns virtual memory address.
	 *
	 * @return The virtual memory address.
	 */
	@Getter
	@EqualsAndHashCode.Include
	@SuppressWarnings("JavaDoc")
	@ToString.Include(name = "virtual", rank = 2)
	private final long virtualAddress;

	/**
	 * -------------- GETTER --------------
	 * Returns the physical memory address.
	 *
	 * @return The physical memory address.
	 */
	@Getter
	@EqualsAndHashCode.Include
	@SuppressWarnings("JavaDoc")
	@ToString.Include(name = "physical", rank = 1)
	private final long physicalAddress;

	/**
	 * Creates a copy of another Ixy's direct memory address specification implementation.
	 *
	 * @param dmaMemory The direct memory address specification to copy.
	 * @return A copy of the {@code dmaMemory}.
	 */
	@SuppressWarnings("MethodReturnOfConcreteClass")
	public static DmaMemory of(IxyDmaMemory dmaMemory) {
		if (dmaMemory == null) throw new InvalidNullParameterException();
		return of(dmaMemory.getVirtualAddress(), dmaMemory.getPhysicalAddress());
	}

}
