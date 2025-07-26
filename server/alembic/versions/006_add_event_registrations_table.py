"""Add event registrations table

Revision ID: 006_add_event_registrations_table
Revises: 005_add_event_images_table
Create Date: 2025-01-19 11:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '006_add_event_registrations_table'
down_revision = '005_add_event_images_table'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create event_registrations table
    op.create_table('event_registrations',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('event_id', sa.Integer(), nullable=False),
    sa.Column('user_id', sa.Integer(), nullable=False),
    sa.Column('registered_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.Column('status', sa.String(), nullable=True),
    sa.ForeignKeyConstraint(['event_id'], ['events.id'], ondelete='CASCADE'),
    sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_event_registrations_id'), 'event_registrations', ['id'], unique=False)


def downgrade() -> None:
    # Drop event_registrations table
    op.drop_index(op.f('ix_event_registrations_id'), table_name='event_registrations')
    op.drop_table('event_registrations')
