package com.lousseief.vault.model

// class representing an association model undergoing change in the UI
/*class AssociationProxy: ItemViewModel<AssociationModel>() {
    val mainIdentifier = bind(AssociationModel::mainIdentifierProperty)
    val secondaryIdentifiers = bind(AssociationModel::secondaryIdentifiersProperty)
    val isNeeded = bind(AssociationModel::isNeededProperty)
    val shouldBeDeactivated = bind(AssociationModel::shouldBeDeactivatedProperty)
    val isDeactivated = bind(AssociationModel::isDeactivatedProperty)
    val category = bind(AssociationModel::categoryProperty)
    val comment = bind(AssociationModel::commentProperty)

    // customized dirty check to allow rollback and commit to work by means of copying lists in which
    // case the model is always marked as dirty from the start
    val dirtyCheck = object : Callable<Boolean> {
            override fun call(): Boolean =
                (
                    item !== null && (
                        mainIdentifier.isDirty ||
                        secondaryIdentifiers.size != item.secondaryIdentifiers.size ||
                        !secondaryIdentifiers.containsAll(item.secondaryIdentifiers) ||
                        isNeeded.isDirty || shouldBeDeactivated.isDirty || isDeactivated.isDirty ||
                        category.isDirty || comment.isDirty

                    )
                )
        }
    override val dirty: BooleanBinding = Bindings.createBooleanBinding(
        dirtyCheck,
        mainIdentifier, secondaryIdentifiers, isNeeded, shouldBeDeactivated, isDeactivated, category, comment)


    private fun copyList() {
        secondaryIdentifiers.value = secondaryIdentifiers.value.map { it }.asObservable()
    }

    fun getCurrentStateAsAssociation() =
        Association(
            mainIdentifier.value,
            secondaryIdentifiers.value,
            isNeeded.value,
            shouldBeDeactivated.value,
            isDeactivated.value,
            category.value,
            comment.value
        )

    init {
        // copies the list after initial binding
        copyList() // this copy is onl needed if we initiate our ItemViewModel with something
        itemProperty.onChange {
            // copies the list whenever the item is changed
            copyList()
        }
    }

    fun rCommit(associationModel: AssociationModel) {
        println("In rCommit")
        println(this.comment.value)
        println(associationModel.comment)
        commit()
        item = associationModel
    }
    // copies list whenever a commit is done so that subsequent commits and rollbacks work as they should (triggered automatically after a commit)
    override fun onCommit() {
        println("In on commit")
        copyList()
        println(this.item?.mainIdentifier)
        println(this.item?.mainIdentifierProperty?.value)
        //item = AssociationModel(getCurrentStateAsAssociation())
    }

    // copies list whenever a rollback is done so that subsequent commits and rollbacks work as they should (must be triggered manually)
    fun onRollback() =
        copyList()

}*/

data class Association(
    var mainIdentifier: String = "",
    var secondaryIdentifiers: MutableList<String> = mutableListOf(),
    var isNeeded: Boolean = true,
    var shouldBeDeactivated: Boolean = false,
    var isDeactivated: Boolean = false,
    var category: String = "",
    var comment: String = "",
) {

    override fun toString() =
        mainIdentifier
}

data class AssociationWithCredentials(
    val association: Association = Association(),
    var credentials: List<Credential> = emptyList()
)
