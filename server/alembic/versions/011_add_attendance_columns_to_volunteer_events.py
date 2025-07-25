"""add attendance columns to volunteer_events

Revision ID: 011
Revises: 010
Create Date: 2025-01-25 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers
revision = '011'
down_revision = '010'
branch_labels = None
depends_on = None

def upgrade():
    # Add attendance tracking columns to volunteer_events table
    op.add_column('volunteer_events', sa.Column('hours_worked', sa.Float, nullable=True))
    op.add_column('volunteer_events', sa.Column('is_approved', sa.Boolean, nullable=True))
    op.add_column('volunteer_events', sa.Column('rejection_reason', sa.Text, nullable=True))
    op.add_column('volunteer_events', sa.Column('karma_points_earned', sa.Integer, nullable=True, default=0))

def downgrade():
    # Remove attendance tracking columns from volunteer_events table
    op.drop_column('volunteer_events', 'karma_points_earned')
    op.drop_column('volunteer_events', 'rejection_reason')
    op.drop_column('volunteer_events', 'is_approved')
    op.drop_column('volunteer_events', 'hours_worked')
