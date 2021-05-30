package server.ecs

class HealthComponent extends Component {
  def damage(amount: Int) = println("Damage")
}

class AdvHealthComponent extends HealthComponent {
  override def damage(amount: Int) = println("A lot of Damage")
}
